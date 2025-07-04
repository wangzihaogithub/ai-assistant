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
import java.time.LocalTime;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Consumer;

/**
 * 阿里云大模型客户端
 * <p>
 * 接口文档：<a href="https://help.aliyun.com/zh/model-studio/call-application-through-api">https://help.aliyun.com/zh/model-studio/call-application-through-api</a>
 * <p>
 * 错误码文档：<a href="https://help.aliyun.com/zh/model-studio/error-code">https://help.aliyun.com/zh/model-studio/error-code</a>
 * <p>
 * 连接池配置文档: <a href="https://help.aliyun.com/zh/model-studio/developer-reference/connection-pool-configuration">https://help.aliyun.com/zh/model-studio/developer-reference/connection-pool-configuration</a>
 * <p>
 * 应用详情：<a href="https://bailian.console.aliyun.com/?tab=app#/app-center/assistant/yourAppId">https://bailian.console.aliyun.com/?tab=app#/app-center/assistant/yourAppId</a>
 */
public class AliyunAppsClient {
    /**
     * 如果被拒绝，则永久重试
     */
    public static final Consumer<Response> IF_REJECT_FOREVER_RETRY = response -> {
        // 是否限流异常 || 是否被工作流拒绝服务 || 阿里云智能体服务超时
        boolean serviceReject = response.isExceptionThrottling() || response.isExceptionModelServiceRejected() || response.isExceptionRequestTimeOutPleaseAgainLater();
        // 是否还有剩余重试次数 && 是否socket超时
        boolean retrySocketTimeout = response.hasRemainRetryCount() && response.isExceptionSocketTimeout();
        // 决定是否重试
        if (serviceReject || retrySocketTimeout) {
            // 重试
            response.retry();
        } else {
            // 不重试, 完成请求
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
    private final ScheduledThreadPoolExecutor scheduled;
    private final String apiKey;
    private final String appId;
    private final ApiKeyStatus apiKeyStatus;
    private Semaphore maxCurrentRequestCount = new Semaphore(2000);
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
        this.scheduled = new ScheduledThreadPoolExecutor(1, r -> {
            Thread thread = new Thread(r);
            thread.setName("AliyunAppsClientRetry-" + appId + "-" + thread.getId());
            return thread;
        });
        scheduled.allowCoreThreadTimeOut(true);
        scheduled.setRemoveOnCancelPolicy(true);
        scheduled.setKeepAliveTime(60, TimeUnit.SECONDS);
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

    public void setMaxCurrentRequestCount(int maxCurrentRequestCount) {
        this.maxCurrentRequestCount = new Semaphore(maxCurrentRequestCount);
    }

    /**
     * 向阿里云发起请求
     *
     * @param maxRetryCount 最大重试次数
     * @return 大模型返回结果 com.alibaba.dashscope.common.DashScopeResult
     */
    public CompletableFuture<DashScopeResult> request(String userMessage, int maxRetryCount) {
        ApplicationParam.ApplicationParamBuilder<?, ?> paramBuilder = ApplicationParam.builder().prompt(userMessage);
        return request(paramBuilder, null, maxRetryCount);
    }

    /**
     * 向阿里云发起请求
     *
     * @param responseContextConsumer 用户重试逻辑
     * @param maxRetryCount           最大重试次数
     * @return 大模型返回结果 com.alibaba.dashscope.common.DashScopeResult
     */
    public CompletableFuture<DashScopeResult> request(String userMessage, Consumer<Response> responseContextConsumer, int maxRetryCount) {
        ApplicationParam.ApplicationParamBuilder<?, ?> paramBuilder = ApplicationParam.builder().prompt(userMessage);
        return request(paramBuilder, responseContextConsumer, maxRetryCount);
    }

    /**
     * 向阿里云发起请求
     *
     * @param paramBuilder            阿里云参数
     * @param responseContextConsumer 用户重试逻辑
     * @param maxRetryCount           最大重试次数
     * @return 大模型返回结果 com.alibaba.dashscope.common.DashScopeResult
     */
    public CompletableFuture<DashScopeResult> request(ApplicationParam.ApplicationParamBuilder<?, ?> paramBuilder, Consumer<Response> responseContextConsumer, int maxRetryCount) {
        // 统计返回耗时
        long timestamp = System.currentTimeMillis();
        apiKeyStatus.beforeRequest();

        CompletableFuture<DashScopeResult> future = new TimeOutCancelCompletableFuture<>();
        try {
            maxCurrentRequestCount.acquire();
        } catch (InterruptedException e) {
            future.completeExceptionally(e);
        }
        // 发起请求阿里云，如果异常自动重试
        requestIfRetry(paramBuilder, future, responseContextConsumer == null ? IF_REJECT_FOREVER_RETRY : responseContextConsumer, maxRetryCount);

        // 统计返回耗时
        future.whenComplete((dashScopeResult, throwable) -> {
            apiKeyStatus.afterRequest(System.currentTimeMillis() - timestamp);
            maxCurrentRequestCount.release();
        });
        return future;
    }

    /**
     * 发起请求阿里云，如果异常自动重试
     *
     * @param paramBuilder            阿里云参数
     * @param future                  CompletableFuture
     * @param responseContextConsumer 用户重试逻辑
     * @param maxRetryCount           最大重试次数
     */
    private void requestIfRetry(ApplicationParam.ApplicationParamBuilder<?, ?> paramBuilder, CompletableFuture<DashScopeResult> future, Consumer<Response> responseContextConsumer, int maxRetryCount) {
        if (future.isDone()) {
            // 外部调用方主动完成或取消任务
            return;
        }
        if (close) {
            future.completeExceptionally(new ClosedChannelException());
            return;
        }
        long startTimestamp = System.currentTimeMillis();
        int remainRetryCount = maxRetryCount - 1;
        try {
            // 若没有配置环境变量，可用百炼API Key将下行替换为：.apiKey("sk-xxx")。但不建议在生产环境中直接将API Key硬编码到代码中，以减少API Key泄露风险。
            client.call(paramBuilder.appId(appId).apiKey(apiKey).build(), new ResultCallback<DashScopeResult>() {
                @Override
                public void onEvent(DashScopeResult message) {
                    // 统计耗时，为限流应该在?秒后重试做数据支撑
                    apiKeyStatus.addOnceRequest(System.currentTimeMillis() - startTimestamp);
                    if (close) {
                        future.complete(message);
                        return;
                    }
                    // 将结果封装为一个Response对象，用户可以Response对象操作异步重试
                    Response responseContext = new Response(paramBuilder, message, null, false, remainRetryCount, future,
                            p -> requestIfRetry(p, future, responseContextConsumer, remainRetryCount),
                            () -> future.complete(message)
                    );
                    try {
                        // 回掉用户传进来的重试逻辑
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
                    // 判断是否被限流
                    boolean throttling = Response.isExceptionThrottling(exception);
                    // 将阿里云的结果封装为一个Response对象，用户可以Response对象操作异步重试
                    Response responseContext = new Response(paramBuilder, null, exception, throttling, remainRetryCount, future,
                            p -> {
                                // mutex两种策略互斥判断
                                AtomicBoolean mutex = new AtomicBoolean();

                                // 计算下次应该几秒后重试
                                int nextRetrySeconds = apiKeyStatus.getNextRetrySeconds(throttling);
                                // 重试策略1: nextRetrySeconds秒后进行重试
                                ScheduledFuture<?> scheduledFuture = scheduled.schedule(() -> {
                                    if (mutex.compareAndSet(false, true)) {
                                        requestIfRetry(p, future, responseContextConsumer, remainRetryCount);
                                    }
                                }, nextRetrySeconds, TimeUnit.SECONDS);

                                //  重试策略2: 当有接口返回后，说明限流-1，可以发起重试。 追加到队列末尾
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
                        // 回掉用户传进来的重试逻辑
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

    /**
     * 将阿里云的结果封装为一个Response对象，用户可以Response对象操作异步重试
     */
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

        /**
         * 是否限流异常
         * 错误码文档：<a href="https://help.aliyun.com/zh/model-studio/error-code">https://help.aliyun.com/zh/model-studio/error-code</a>
         *
         * @param exception 异常
         * @return true=限流异常，false=不是
         */
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

        public boolean isException() {
            return exception != null;
        }

        public int getRemainRetryCount() {
            return remainRetryCount;
        }

        /**
         * 是否还有剩余重试次数
         *
         * @return true=还有重试次数，false=没有
         */
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
        public boolean isExceptionSocketTimeout() {
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
         * 是否阿里云智能体服务超时
         * Caused by: com.alibaba.dashscope.exception.ApiException: {"statusCode":500,"message":"Request timed out, please try again later.","code":"RequestTimeOut","isJson":true,"requestId":"4cdd9eef-a0cd-90d3-8b7a-0c673144a93c"}; status body:{"statusCode":500,"message":"Request timed out, please try again later.","code":"RequestTimeOut","isJson":true,"requestId":"4cdd9eef-a0cd-90d3-8b7a-0c673144a93c"}
         * 阿里云服务支持:(当前由真人提供服务（工程师是杜依纯，非机器人）): 您好，这个考虑是服务器并发满了，请求拒绝了.
         *
         * @return true=智能体服务超时，false=不是
         */
        public boolean isExceptionRequestTimeOutPleaseAgainLater() {
            String message;
            return exception instanceof ApiException
                    && (message = Objects.toString(exception.getMessage(), "")).contains("RequestTimeOut")
                    && message.contains("please try again later");
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

        /**
         * 重试
         */
        public void retry() {
            retry.accept(paramBuilder);
        }

        /**
         * 完成请求，不重试
         */
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

    /**
     * 统计ApiKey的请求状态，为限流应该在何时重试做数据支撑
     */
    private static class ApiKeyStatus {
        final String apiKey;
        final LinkedList<RequestListener> listeners = new LinkedList<>();
        // 当前请求未完成的数量
        final AtomicInteger currentRequestCount = new AtomicInteger();
        // 平均完成用户完整请求的耗时，一次用户完整请求如果出发限流会包含多次阿里云调用的耗时
        volatile long avgRequestMs = 0;
        // 平均一次阿里云调用的耗时
        volatile long avgOnceRequestMs = 0;
        private int secondIncr = 0;

        private ApiKeyStatus(String apiKey) {
            this.apiKey = apiKey;
        }

        /**
         * 计算下次应该几秒后重试
         *
         * @param isThrottling 是否限流
         * @return 下次应该N秒后重试
         */
        public int getNextRetrySeconds(boolean isThrottling) {
            int nextRetrySeconds;
            // 有其他请求还没回来
            if (listeners.size() > 1) {
                long avgS = avgOnceRequestMs / 1000;
                nextRetrySeconds = ThreadLocalRandom.current().nextInt((int) Math.max(30, avgS * 0.8), (int) Math.max(60, avgS));
            } else if (isThrottling) {
                nextRetrySeconds = ThreadLocalRandom.current().nextInt(1, 6);
            } else {
                return 0;
            }
            return nextRetrySeconds + (secondIncr++ % 60);
        }

        /**
         * 重试策略2: 当有接口返回后，说明限流-1，可以发起重试。 追加到队列末尾
         *
         * @param listener 监听有接口返回后
         */
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

        @Override
        public String toString() {
            return "ApiKeyStatus{" +
                    "apiKey='" + apiKey + '\'' +
                    ", currentRequestCount=" + currentRequestCount +
                    ", avgOnceRequestMs=" + (avgRequestMs / 1000) +
                    ", avgRequestSec=" + (avgRequestMs / 1000) +
                    '}';
        }
    }
}
