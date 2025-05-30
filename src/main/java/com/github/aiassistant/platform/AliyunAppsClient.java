package com.github.aiassistant.platform;

import com.alibaba.dashscope.api.SynchronizeHalfDuplexApi;
import com.alibaba.dashscope.app.ApplicationParam;
import com.alibaba.dashscope.base.HalfDuplexParamBase;
import com.alibaba.dashscope.common.DashScopeResult;
import com.alibaba.dashscope.common.OutputMode;
import com.alibaba.dashscope.common.ResultCallback;
import com.alibaba.dashscope.protocol.ApiServiceOption;
import com.alibaba.dashscope.protocol.ConnectionConfigurations;
import com.alibaba.dashscope.protocol.HttpMethod;
import com.alibaba.dashscope.protocol.StreamingMode;
import com.alibaba.dashscope.utils.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    public static final Consumer<Response> DEFAULT_RETRY_CONSUMER = retry -> {
        if (retry.isThrottling()) {
            retry.retry();
        } else {
            retry.exit();
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
            thread.setName(appId + ".retry" + thread.getId());
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
        return request(paramBuilder, DEFAULT_RETRY_CONSUMER, maxRetryCount);
    }

    public CompletableFuture<DashScopeResult> request(String userMessage, Consumer<Response> consumer, int maxRetryCount) {
        ApplicationParam.ApplicationParamBuilder<?, ?> paramBuilder = ApplicationParam.builder().prompt(userMessage);
        return request(paramBuilder, consumer, maxRetryCount);
    }

    public CompletableFuture<DashScopeResult> request(ApplicationParam.ApplicationParamBuilder<?, ?> paramBuilder, Consumer<Response> consumer, int maxRetryCount) {
        long timestamp = System.currentTimeMillis();
        apiKeyStatus.beforeRequest();
        CompletableFuture<DashScopeResult> future = new TimeOutCancelCompletableFuture<>();
        requestIfRetry(paramBuilder, future, consumer == null ? DEFAULT_RETRY_CONSUMER : consumer, maxRetryCount);
        future.whenComplete((dashScopeResult, throwable) -> apiKeyStatus.afterRequest(System.currentTimeMillis() - timestamp));
        return future;
    }

    private void requestIfRetry(ApplicationParam.ApplicationParamBuilder<?, ?> paramBuilder, CompletableFuture<DashScopeResult> future, Consumer<Response> responseContextConsumer, int maxRetryCount) {
        if (future.isDone()) {
            // 外部调用方主动完成或取消任务
            return;
        }
        int remainRetryCount = maxRetryCount - 1;
        try {
            // 若没有配置环境变量，可用百炼API Key将下行替换为：.apiKey("sk-xxx")。但不建议在生产环境中直接将API Key硬编码到代码中，以减少API Key泄露风险。
            client.call(paramBuilder.appId(appId).apiKey(apiKey).build(), new ResultCallback<DashScopeResult>() {
                @Override
                public void onEvent(DashScopeResult message) {
                    if (remainRetryCount > 0) {
                        Response responseContext = new Response(paramBuilder, message, null, false, remainRetryCount,
                                p -> requestIfRetry(p, future, responseContextConsumer, remainRetryCount),
                                () -> future.complete(message)
                        );
                        try {
                            responseContextConsumer.accept(responseContext);
                        } catch (Throwable e) {
                            future.completeExceptionally(e);
                        }
                    } else {
                        future.complete(message);
                    }
                }

                @Override
                public void onComplete() {

                }

                @Override
                public void onError(Exception exception) {
                    if (remainRetryCount > 0) {
                        boolean throttling = Response.isThrottling(exception);
                        Response responseContext = new Response(paramBuilder, null, exception, throttling, remainRetryCount,
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
                    } else {
                        future.completeExceptionally(exception);
                    }
                }
            });
        } catch (Throwable throwable) {
            future.completeExceptionally(throwable);
        }
    }

    private interface RequestListener {
        void complete();
    }

    public static class Response {
        final DashScopeResult result;
        final Exception exception;
        final int remainRetryCount;
        final boolean throttling;
        final Consumer<ApplicationParam.ApplicationParamBuilder<?, ?>> retry;
        final Runnable exit;
        ApplicationParam.ApplicationParamBuilder<?, ?> paramBuilder;

        public Response(ApplicationParam.ApplicationParamBuilder<?, ?> paramBuilder, DashScopeResult result, Exception exception, boolean throttling, int remainRetryCount, Consumer<ApplicationParam.ApplicationParamBuilder<?, ?>> retry, Runnable exit) {
            this.paramBuilder = paramBuilder;
            this.result = result;
            this.exception = exception;
            this.throttling = throttling;
            this.remainRetryCount = remainRetryCount;
            this.retry = retry;
            this.exit = exit;
        }

        public static boolean isThrottling(Exception exception) {
            if (exception != null) {
                String errorString = Objects.toString(exception.getMessage(), "");
                // 限流异常
                // 错误码文档：https://help.aliyun.com/zh/model-studio/error-code
                return errorString.contains("Throttling");
            } else {
                return false;
            }
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
                exit.run();
            }
        }

        public DashScopeResult getResult() {
            return result;
        }

        public Runnable getExit() {
            return exit;
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

        public void exit() {
            exit.run();
        }

        public int getRemainRetryCount() {
            return remainRetryCount;
        }

        public boolean isThrottling() {
            return throttling;
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

        private ApiKeyStatus(String apiKey) {
            this.apiKey = apiKey;
        }

        @Override
        public String toString() {
            return "ApiKeyStatus{" +
                    "apiKey='" + apiKey + '\'' +
                    ", currentRequestCount=" + currentRequestCount +
                    ", avgRequestSec=" + (avgRequestMs / 1000) +
                    '}';
        }

        public int getNextRetrySeconds(boolean isThrottling) {
            // 有其他请求还没回来
            if (currentRequestCount.intValue() > 1) {
                long avgS = avgRequestMs / 1000;
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
