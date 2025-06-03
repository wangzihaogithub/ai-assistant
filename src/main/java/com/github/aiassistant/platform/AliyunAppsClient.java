package com.github.aiassistant.platform;

import com.alibaba.dashscope.api.SynchronizeHalfDuplexApi;
import com.alibaba.dashscope.app.ApplicationParam;
import com.alibaba.dashscope.base.HalfDuplexParamBase;
import com.alibaba.dashscope.common.DashScopeResult;
import com.alibaba.dashscope.common.OutputMode;
import com.alibaba.dashscope.common.ResultCallback;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.protocol.ApiServiceOption;
import com.alibaba.dashscope.protocol.ConnectionConfigurations;
import com.alibaba.dashscope.protocol.HttpMethod;
import com.alibaba.dashscope.protocol.StreamingMode;
import com.alibaba.dashscope.utils.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketTimeoutException;
import java.nio.channels.ClosedChannelException;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * 阿里云大模型客户端
 * 接口文档：https://help.aliyun.com/zh/model-studio/call-application-through-api
 * 错误码文档：https://help.aliyun.com/zh/model-studio/error-code
 * 连接池配置文档: https://help.aliyun.com/zh/model-studio/developer-reference/connection-pool-configuration
 * 应用详情：https://bailian.console.aliyun.com/?tab=app#/app-center/assistant/{appId}
 */
public class AliyunAppsClient {
    /**
     * 如果被拒绝，则永久重试
     */
    public static final Consumer<Response> IF_REJECT_FOREVER_RETRY = response -> {
        if (response.isExceptionThrottling() || response.isExceptionModelServiceRejected()) {
            response.retry();
        } else if (response.hasRemainRetryCount() && response.isExceptionTimeout()) {
            response.retry();
        } else {
            response.complete();
        }
    };
    private static final Map<String, ApiKeyStatus> API_KEY_STATUS_MAP = new ConcurrentHashMap<>();

    //    static {
    // https://help.aliyun.com/zh/model-studio/developer-reference/connection-pool-configuration
    // 以下代码示例展示了如何配置连接池相关参数（如超时时间、最大连接数等），并调用大模型服务。您可以根据实际需求调整相关参数，以优化并发性能和资源利用率。
    // 连接池配置
//        setConnectionConfigurations(
//                ConnectionConfigurations.builder()
//                        .connectTimeout(Duration.ofSeconds(10))  // 建立连接的超时时间, 默认 120s
//                        .readTimeout(Duration.ofSeconds(300)) // 读取数据的超时时间, 默认 300s
//                        .writeTimeout(Duration.ofSeconds(60)) // 写入数据的超时时间, 默认 60s
//                        .connectionIdleTimeout(Duration.ofSeconds(300)) // 连接池中空闲连接的超时时间, 默认 300s
//                        .connectionPoolSize(256) // 连接池中的最大连接数, 默认 32
//                        .maximumAsyncRequests(256)  // 最大并发请求数, 默认 32
//                        .maximumAsyncRequestsPerHost(256) // 单个主机的最大并发请求数, 默认 32
//                        .build());
//    }
    private static final Logger log = LoggerFactory.getLogger(AliyunAppsClient.class);
    private final SynchronizeHalfDuplexApi<HalfDuplexParamBase> client;
    private final ScheduledExecutorService scheduled;
    private final String apiKey;
    private final String appId;
    private final ApiKeyStatus apiKeyStatus;
    private boolean close = false;

    public AliyunAppsClient(String apiKey, String appId) {
        this(apiKey, appId, ApiServiceOption.builder()
                .httpMethod(HttpMethod.POST)
                .outputMode(OutputMode.ACCUMULATE)
                .isSSE(false)
                .isService(false)
                .task(appId)
                .taskGroup("apps")
                .function("completion")
                .streamingMode(StreamingMode.NONE)
                .build());
    }

    public AliyunAppsClient(String apiKey, String appId, ApiServiceOption option) {
        this.apiKey = apiKey;
        this.appId = appId;
        this.client = new SynchronizeHalfDuplexApi<>(option);
        this.scheduled = Executors.newScheduledThreadPool(1, r -> {
            Thread thread = new Thread(r);
            thread.setName("AliyunAppsClientRetry-" + appId + "-" + thread.getId());
            return thread;
        });
        this.apiKeyStatus = API_KEY_STATUS_MAP.computeIfAbsent(apiKey, ApiKeyStatus::new);
    }

    public static void setDefaultConnectionConfigurations() {
        setConnectionConfigurations(ConnectionConfigurations.builder().build());
    }

    public static void setConnectionConfigurations(ConnectionConfigurations connectionConfigurations) {
        // https://help.aliyun.com/zh/model-studio/developer-reference/connection-pool-configuration
        // 以下代码示例展示了如何配置连接池相关参数（如超时时间、最大连接数等），并调用大模型服务。您可以根据实际需求调整相关参数，以优化并发性能和资源利用率。
        // 连接池配置
        Constants.connectionConfigurations = connectionConfigurations;
    }

    public CompletableFuture<DashScopeResult> request(String userMessage, int maxRetryCount) {
        ApplicationParam.ApplicationParamBuilder<?, ?> paramBuilder = ApplicationParam.builder().prompt(userMessage);
        return request(paramBuilder, null, maxRetryCount);
    }

    public CompletableFuture<DashScopeResult> request(String userMessage, Consumer<Response> consumer, int maxRetryCount) {
        ApplicationParam.ApplicationParamBuilder<?, ?> paramBuilder = ApplicationParam.builder().prompt(userMessage);
        return request(paramBuilder, consumer, maxRetryCount);
    }

    public CompletableFuture<DashScopeResult> request(ApplicationParam.ApplicationParamBuilder<?, ?> paramBuilder, Consumer<Response> consumer, int maxRetryCount) {
        if (close) {
            CompletableFuture<DashScopeResult> future = new CompletableFuture<>();
            future.completeExceptionally(new ClosedChannelException());
            return future;
        }
        long timestamp = System.currentTimeMillis();
        apiKeyStatus.beforeRequest();
        CompletableFuture<DashScopeResult> future = new TimeOutCancelCompletableFuture<>();
        requestIfRetry(paramBuilder, future, consumer == null ? IF_REJECT_FOREVER_RETRY : consumer, maxRetryCount);
        future.whenComplete((dashScopeResult, throwable) -> apiKeyStatus.afterRequest(System.currentTimeMillis() - timestamp));
        return future;
    }

    private void requestIfRetry(ApplicationParam.ApplicationParamBuilder<?, ?> paramBuilder, CompletableFuture<DashScopeResult> future, Consumer<Response> responseContextConsumer, int maxRetryCount) {
        if (future.isDone()) {
            // 外部调用方主动完成或取消任务
            return;
        }
        long startTimestamp = System.currentTimeMillis();
        int remainRetryCount = maxRetryCount - 1;
        try {
            // 若没有配置环境变量，可用百炼API Key将下行替换为：.apiKey("sk-xxx")。但不建议在生产环境中直接将API Key硬编码到代码中，以减少API Key泄露风险。
            client.call(paramBuilder.appId(appId).apiKey(apiKey).build(), new ResultCallback<DashScopeResult>() {
                @Override
                public void onEvent(DashScopeResult message) {
                    apiKeyStatus.addOnceRequest(System.currentTimeMillis() - startTimestamp);
                    if (close) {
                        future.complete(message);
                        return;
                    }
                    Response responseContext = new Response(paramBuilder, message, null, false, remainRetryCount, future,
                            p -> requestIfRetry(p, future, responseContextConsumer, remainRetryCount),
                            () -> future.complete(message)
                    );
                    try {
                        responseContextConsumer.accept(responseContext);
                    } catch (Throwable e) {
                        future.completeExceptionally(e);
                    }
                }

                @Override
                public void onComplete() {

                }

                @Override
                public void onError(Exception exception) {
                    if (close) {
                        future.completeExceptionally(exception);
                        return;
                    }
                    boolean throttling = Response.isExceptionThrottling(exception);
                    Response responseContext = new Response(paramBuilder, null, exception, throttling, remainRetryCount, future,
                            p -> {
                                AtomicBoolean mutex = new AtomicBoolean();
                                ScheduledFuture<?> scheduledFuture = scheduled.schedule(() -> {
                                    if (mutex.compareAndSet(false, true)) {
                                        requestIfRetry(p, future, responseContextConsumer, remainRetryCount);
                                    }
                                }, apiKeyStatus.getNextRetrySeconds(throttling), TimeUnit.SECONDS);
                                // 追加到末尾
                                apiKeyStatus.addListener(() -> {
                                    if (mutex.compareAndSet(false, true)) {
                                        scheduledFuture.cancel(false);
                                        requestIfRetry(p, future, responseContextConsumer, remainRetryCount);
                                    }
                                });
                            },
                            () -> future.completeExceptionally(exception)
                    );
                    try {
                        responseContextConsumer.accept(responseContext);
                    } catch (Throwable e) {
                        future.completeExceptionally(e);
                    }
                }
            });
        } catch (Throwable throwable) {
            future.completeExceptionally(throwable);
        }
    }

    public void close() {
        this.close = true;
    }

    private interface RequestListener {
        void complete();
    }

    public static class Response {
        final DashScopeResult result;
        final Exception exception;
        final int remainRetryCount;
        final boolean exceptionThrottling;
        final Consumer<ApplicationParam.ApplicationParamBuilder<?, ?>> retry;
        final Runnable complete;
        final CompletableFuture<DashScopeResult> future;
        ApplicationParam.ApplicationParamBuilder<?, ?> paramBuilder;

        public Response(ApplicationParam.ApplicationParamBuilder<?, ?> paramBuilder, DashScopeResult result, Exception exception, boolean exceptionThrottling, int remainRetryCount,
                        CompletableFuture<DashScopeResult> future,
                        Consumer<ApplicationParam.ApplicationParamBuilder<?, ?>> retry, Runnable complete) {
            this.paramBuilder = paramBuilder;
            this.result = result;
            this.exception = exception;
            this.exceptionThrottling = exceptionThrottling;
            this.remainRetryCount = remainRetryCount;
            this.future = future;
            this.retry = retry;
            this.complete = complete;
        }

        public static boolean isExceptionThrottling(Exception exception) {
            if (exception != null) {
                String errorString = Objects.toString(exception.getMessage(), "");
                // 限流异常
                // 错误码文档：https://help.aliyun.com/zh/model-studio/error-code
                return errorString.contains("Throttling");
            } else {
                return false;
            }
        }

        public int getRemainRetryCount() {
            return remainRetryCount;
        }

        public boolean hasRemainRetryCount() {
            return remainRetryCount > 0;
        }

        /**
         * 是否限流异常
         *
         * @return true=限流异常，false=不是
         */
        public boolean isExceptionThrottling() {
            return exceptionThrottling;
        }

        /**
         * 是否接口超时异常
         *
         * @return true=接口超时异常，false=不是
         */
        public boolean isExceptionTimeout() {
            return exception instanceof SocketTimeoutException;
        }

        /**
         * 是否被工作流拒绝服务
         * {"statusCode":500,"message":"{\"nodeName\":\"大模型1\",\"errorInfo\":\" 400  InternalError.Algo: An error occurred in model serving, error message is: [!]\",\"nodeId\":\"LLM_Mqdp\"}","code":"ModelServiceFailed","isJson":true,"requestId":"5ea0d282-5513-9e7c-9b8f-729c9a448441"}; status body:{"statusCode":500,"message":"{\"nodeName\":\"大模型1\",\"errorInfo\":\" 400  InternalError.Algo: An error occurred in model serving, error message is: [Request rejected by inference engine!]\",\"nodeId\":\"LLM_Mqdp\"}",
         * "code":"ModelServiceFailed","isJson":true,"requestId":"xxxx"}
         *
         * @return true=工作流拒绝服务，false=不是
         */
        public boolean isExceptionModelServiceRejected() {
            return exception instanceof ApiException
                    && Objects.toString(exception.getMessage(), "").contains("Request rejected by inference engine");
        }

        /**
         * 是否安全拦截异常
         *
         * @return true=安全拦截异常，false=不是
         */
        public boolean isExceptionDataInspectionFailed() {
            return exception instanceof ApiException
                    && Objects.toString(exception.getMessage(), "").contains("DataInspectionFailed");
        }

        public CompletableFuture<DashScopeResult> getFuture() {
            return future;
        }

        public ApplicationParam.ApplicationParamBuilder<?, ?> getParamBuilder() {
            return paramBuilder;
        }

        public void setParamBuilder(ApplicationParam.ApplicationParamBuilder<?, ?> paramBuilder) {
            this.paramBuilder = paramBuilder;
        }

        public void run(boolean needRetry) {
            if (needRetry) {
                retry.accept(paramBuilder);
            } else {
                complete.run();
            }
        }

        public DashScopeResult getResult() {
            return result;
        }

        public Runnable getComplete() {
            return complete;
        }

        public Consumer<ApplicationParam.ApplicationParamBuilder<?, ?>> getRetry() {
            return retry;
        }

        public Exception getException() {
            return exception;
        }

        public void retry() {
            retry.accept(paramBuilder);
        }

        public void complete() {
            complete.run();
        }

    }

    private static class TimeOutCancelCompletableFuture<T> extends CompletableFuture<T> {
        @Override
        public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            try {
                return super.get(timeout, unit);
            } catch (TimeoutException t) {
                cancel(false);
                throw t;
            }
        }
    }

    private static class ApiKeyStatus {
        final String apiKey;
        final AtomicInteger currentRequestCount = new AtomicInteger();
        final LinkedList<RequestListener> listeners = new LinkedList<>();
        volatile long avgRequestMs = 0;
        volatile long avgOnceRequestMs = 0;

        private ApiKeyStatus(String apiKey) {
            this.apiKey = apiKey;
        }

        @Override
        public String toString() {
            return "ApiKeyStatus{" +
                    "apiKey='" + apiKey + '\'' +
                    ", currentRequestCount=" + currentRequestCount +
                    ", avgOnceRequestMs=" + (avgRequestMs / 1000) +
                    ", avgRequestSec=" + (avgRequestMs / 1000) +
                    '}';
        }

        public int getNextRetrySeconds(boolean isThrottling) {
            // 有其他请求还没回来
            if (listeners.size() > 1) {
                long avgS = avgOnceRequestMs / 1000;
                return ThreadLocalRandom.current().nextInt((int) Math.max(30, avgS * 0.8), (int) Math.max(60, avgS));
            } else if (isThrottling) {
                return ThreadLocalRandom.current().nextInt(1, 6);
            } else {
                return 0;
            }
        }

        public void addListener(RequestListener listener) {
            // 追加到末尾
            synchronized (listeners) {
                listeners.addLast(listener);
            }
        }

        public void beforeRequest() {
            currentRequestCount.incrementAndGet();
        }

        public void addOnceRequest(long ms) {
            long m = this.avgOnceRequestMs;
            this.avgOnceRequestMs = m == 0 ? ms : (m + ms) / 2;
        }

        public void afterRequest(long ms) {
            currentRequestCount.decrementAndGet();
            synchronized (listeners) {
                long m = this.avgRequestMs;
                this.avgRequestMs = m == 0 ? ms : (m + ms) / 2;
                // 从前往后叫醒
                RequestListener poll = listeners.poll();
                if (poll != null) {
                    try {
                        poll.complete();
                    } catch (Exception e) {
                        log.warn("ApiKeyStatus afterRequest error {}", e.toString(), e);
                    }
                }
            }
        }
    }
}
