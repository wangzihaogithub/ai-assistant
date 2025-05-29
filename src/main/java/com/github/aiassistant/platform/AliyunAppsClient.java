package com.github.aiassistant.platform;

import com.alibaba.dashscope.api.SynchronizeHalfDuplexApi;
import com.alibaba.dashscope.app.ApplicationParam;
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

/**
 * 阿里云大模型客户端
 * 接口文档：https://help.aliyun.com/zh/model-studio/call-application-through-api
 * 错误码文档：https://help.aliyun.com/zh/model-studio/error-code
 * 连接池配置文档: https://help.aliyun.com/zh/model-studio/developer-reference/connection-pool-configuration
 * 应用详情：https://bailian.console.aliyun.com/?tab=app#/app-center/assistant/{appId}
 */
public class AliyunAppsClient {
    private static final Map<String, ApiKeyStatus> API_KEY_STATUS_MAP = new ConcurrentHashMap<>();
    private static final Logger log = LoggerFactory.getLogger(AliyunAppsClient.class);

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

    private final SynchronizeHalfDuplexApi<ApplicationParam> client;
    private final ScheduledExecutorService scheduled;
    private final String apiKey;
    private final String appId;
    private final ApiKeyStatus apiKeyStatus;

    public AliyunAppsClient(String apiKey, String appId) {
        this.apiKey = apiKey;
        this.appId = appId;
        this.client = new SynchronizeHalfDuplexApi<>(ApiServiceOption.builder()
                .httpMethod(HttpMethod.POST)
                .outputMode(OutputMode.ACCUMULATE)
                .isSSE(false)
                .isService(false)
                .task(appId)
                .taskGroup("apps")
                .function("completion")
                .streamingMode(StreamingMode.NONE)
                .build());
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
        ApplicationParam param = ApplicationParam.builder()
                // 若没有配置环境变量，可用百炼API Key将下行替换为：.apiKey("sk-xxx")。但不建议在生产环境中直接将API Key硬编码到代码中，以减少API Key泄露风险。
                .apiKey(apiKey)
                .appId(appId)
                .prompt(userMessage)
                .build();
        CompletableFuture<DashScopeResult> future = new CompletableFuture<>();
        apiKeyStatus.beforeRequest();
        requestIfRetry(param, future, maxRetryCount);
        future.whenComplete((dashScopeResult, throwable) -> apiKeyStatus.afterRequest());
        return future;
    }

    private void requestIfRetry(ApplicationParam param, CompletableFuture<DashScopeResult> future, int maxRetryCount) {
        if (future.isDone()) {
            // 外部调用方主动完成或取消任务
            return;
        }
        try {
            client.call(param, new ResultCallback<DashScopeResult>() {
                @Override
                public void onEvent(DashScopeResult message) {
                    future.complete(message);
                }

                @Override
                public void onComplete() {

                }

                @Override
                public void onError(Exception exception) {
                    String errorString = Objects.toString(exception.getMessage(), "");
                    // 限流异常
                    // 错误码文档：https://help.aliyun.com/zh/model-studio/error-code
                    if (maxRetryCount > 0 && errorString.contains("Throttling")) {
                        AtomicBoolean mutex = new AtomicBoolean();
                        ScheduledFuture<?> scheduledFuture = scheduled.schedule(() -> {
                            if (mutex.compareAndSet(false, true)) {
                                requestIfRetry(param, future, maxRetryCount - 1);
                            }
                        }, apiKeyStatus.getNextRetrySeconds(), TimeUnit.SECONDS);
                        // 追加到末尾
                        apiKeyStatus.addListener(() -> {
                            if (mutex.compareAndSet(false, true)) {
                                scheduledFuture.cancel(false);
                                requestIfRetry(param, future, maxRetryCount - 1);
                            }
                        });
                    } else {
                        future.completeExceptionally(exception);
                    }
                }
            });
        } catch (Throwable throwable) {
            future.completeExceptionally(throwable);
        }
    }

    interface RequestListener {
        void complete();
    }

    private static class ApiKeyStatus {
        final String apiKey;
        final AtomicInteger currentRequestCount = new AtomicInteger();
        final LinkedList<RequestListener> listeners = new LinkedList<>();

        private ApiKeyStatus(String apiKey) {
            this.apiKey = apiKey;
        }

        @Override
        public String toString() {
            return "ApiKeyStatus{" +
                    "apiKey='" + apiKey + '\'' +
                    ", currentRequestCount=" + currentRequestCount +
                    '}';
        }

        public int getNextRetrySeconds() {
            // 有其他请求还没回来
            if (currentRequestCount.intValue() > 1) {
                return ThreadLocalRandom.current().nextInt(30, 60);
            } else {
                return ThreadLocalRandom.current().nextInt(3, 6);
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

        public void afterRequest() {
            currentRequestCount.decrementAndGet();
            synchronized (listeners) {
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
