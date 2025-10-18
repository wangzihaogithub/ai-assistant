package dev.langchain4j.model.openai;

import com.github.aiassistant.entity.model.langchain4j.ThinkingAiMessage;
import com.github.aiassistant.platform.JsonUtil;
import com.github.aiassistant.service.text.GenerateRequest;
import com.github.aiassistant.service.text.StreamingResponseHandlerAdapter;
import dev.ai4j.openai4j.OpenAiHttpException;
import dev.ai4j.openai4j.chat.*;
import dev.ai4j.openai4j.shared.StreamOptions;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.StreamingResponseHandler;
import dev.langchain4j.model.chat.listener.*;
import dev.langchain4j.model.chat.request.json.JsonSchema;
import dev.langchain4j.model.output.Response;
import okhttp3.*;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;
import okhttp3.sse.EventSources;
import okio.BufferedSink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Proxy;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static dev.langchain4j.model.openai.InternalOpenAiHelper.*;
import static java.time.Duration.ofSeconds;
import static java.util.Collections.emptyList;

/**
 * 通义千问API接口文档：<a href="https://help.aliyun.com/zh/model-studio/use-qwen-by-calling-api">https://help.aliyun.com/zh/model-studio/use-qwen-by-calling-api</a>
 * <p>
 * Represents an OpenAI language model with a chat completion interface, such as gpt-3.5-turbo and gpt-4.
 * The model's response is streamed token by token and should be handled with {@link StreamingResponseHandler}.
 * You can find description of parameters <a href="https://platform.openai.com/docs/api-reference/chat/create">here</a>.
 */
public class OpenAiChatClient {
    private static final Logger log = LoggerFactory.getLogger(OpenAiChatClient.class);
    private static final MediaType mediaType = MediaType.get("application/json; charset=utf-8");
    private final OkHttpClient client;
    private final Boolean strictTools;
    private final Boolean strictJsonSchema;
    private final String modelName;
    private final ResponseFormatType responseFormat;
    private final List<ChatModelListener> listeners;
    private final Supplier<ChatCompletionRequest.Builder> requestBuilder;
    private final String endpoint;
    private final JsonUtil.ObjectWriter objectWriter = JsonUtil.objectWriter();
    private final JsonUtil.ObjectReader objectReader = JsonUtil.objectReader();
    private boolean destroy;

    public OpenAiChatClient(String baseUrl,
                            String apiKey,
                            String modelName,
                            Double temperature,
                            Double topP,
                            List<String> stop,
                            /**
                             * 本次请求返回的最大 Token 数。
                             * max_tokens 的设置不会影响大模型的生成过程，如果模型生成的 Token 数超过max_tokens，本次请求会返回截断后的内容。
                             * 默认值和最大值都是模型的最大输出长度。关于各模型的最大输出长度，请参见模型列表。
                             * max_tokens参数适用于需要限制字数（如生成摘要、关键词）、控制成本或减少响应时间的场景。
                             * 对于qwen-vl-ocr-2025-04-13、qwen-vl-ocr-latest模型，max_tokens默认值为2048，最大值为8192。
                             * 对于 QwQ、QVQ 与开启思考模式的 Qwen3 模型，max_tokens会限制回复内容的长度，不限制深度思考内容的长度。
                             */
                            Integer maxCompletionTokens,
                            /**
                             * 控制模型生成文本时的内容重复度。
                             * 取值范围：[-2.0, 2.0]。正数会减少重复度，负数会增加重复度。
                             * 适用场景：
                             * 较高的presence_penalty适用于要求多样性、趣味性或创造性的场景，如创意写作或头脑风暴。
                             * 较低的presence_penalty适用于要求一致性或专业术语的场景，如技术文档或其他正式文档。
                             */
                            Double presencePenalty,
                            /**
                             * frequency_penalty的取值范围通常在0到2之间。
                             * 较高的frequency_penalty值会使模型更倾向于使用新的词汇，降低重复度；
                             * 而较低的值则可能导致模型生成更多重复内容‌
                             */
                            Double frequencyPenalty,
                            Map<String, Integer> logitBias,
                            String responseFormat,
                            /**
                             * 设置seed参数会使文本生成过程更具有确定性，通常用于使模型每次运行的结果一致。
                             * 在每次模型调用时传入相同的seed值（由您指定），并保持其他参数不变，模型将尽可能返回相同的结果。
                             * 取值范围：0到231−1。
                             */
                            Integer seed,

                            String user,
                            Boolean strictTools,
                            Boolean parallelToolCalls,
                            Duration timeout,
                            Duration connectTimeout,
                            Proxy proxy,
                            Map<String, String> customHeaders,
                            /**
                             * （可选） 默认值为1
                             * 生成响应的个数，取值范围是1-4。对于需要生成多个响应的场景（如创意写作、广告文案等），可以设置较大的 n 值。
                             * 当前仅支持 qwen-plus 与 Qwen3（非思考模式） 模型，且在传入 tools 参数时固定为1。
                             * 设置较大的 n 值不会增加输入 Token 消耗，会增加输出 Token 的消耗。
                             */
                            Integer n,
                            List<ChatModelListener> listeners,
                            Boolean strictJsonSchema) {
        this.endpoint = baseUrl + (baseUrl.endsWith("/") ? "chat/completions" : "/chat/completions");

        timeout = timeout != null ? timeout : ofSeconds(60);

        OkHttpClient.Builder okHttpClientBuilder = new OkHttpClient.Builder()
                .callTimeout(timeout)
                .connectTimeout(connectTimeout == null ? timeout : connectTimeout)
                .readTimeout(timeout)
                .writeTimeout(timeout);

        Map<String, String> headers = new HashMap<>();
        headers.put("User-Agent", "langchain4j-openai");
        if (customHeaders != null) {
            headers.putAll(customHeaders);
        }
        if (apiKey != null && !apiKey.isEmpty()) {
            headers.put("Authorization", "Bearer " + apiKey);
        }
        headers.put("api-key", apiKey);
        headers.put("x-api-key", apiKey);
        okHttpClientBuilder.addInterceptor(chain -> {
            Request.Builder builder = chain.request().newBuilder();
            headers.forEach(builder::addHeader);
            return chain.proceed(builder.build());
        });
        if (proxy != null) {
            okHttpClientBuilder.proxy(proxy);
        }
        okHttpClientBuilder.dispatcher(new Dispatcher(new ThreadPoolExecutor(0, Integer.MAX_VALUE, 60, TimeUnit.SECONDS,
                new SynchronousQueue<>(), runnable -> {
            Thread result = new Thread(runnable);
            result.setName("OkHttp ai-chat-" + result.getId());
            result.setDaemon(false);
            return result;
        })));
        this.client = okHttpClientBuilder.build();

        this.modelName = modelName;
        this.responseFormat = responseFormat != null ? ResponseFormatType.valueOf(responseFormat.toUpperCase(Locale.ROOT)) : null;
        this.strictTools = strictTools != null && strictTools;
        this.strictJsonSchema = strictJsonSchema;
        this.listeners = listeners == null ? emptyList() : new ArrayList<>(listeners);
        this.requestBuilder = () -> ChatCompletionRequest.builder().stream(true)
                .streamOptions(StreamOptions.builder()
                        .includeUsage(true)
                        .build())
                .model(modelName)
                .temperature(temperature)
                .n(n)
                .topP(topP)
                .stop(stop)
                .maxCompletionTokens(maxCompletionTokens)
                .presencePenalty(presencePenalty)
                .frequencyPenalty(frequencyPenalty)
                .logitBias(logitBias)
                .seed(seed)
                .user(user)
                .parallelToolCalls(parallelToolCalls);
    }

    private static void handle(ChatCompletionResponse partialResponse,
                               StreamingResponseHandler<AiMessage> handler,
                               OpenAiStreamingResponseBuilder responseBuilder) {
        List<ChatCompletionChoice> choices = partialResponse.choices();
        if (choices == null || choices.isEmpty()) {
            return;
        }
        ChatCompletionChoice choice = choices.get(0);
        if (choice == null) {
            return;
        }
        Delta delta = choice.delta();
        if (delta == null) {
            return;
        }

        // h.onAudioChunk
        Delta.Audio audio = delta.audio();
        if (audio != null && handler instanceof AudioStreamingResponseHandler) {
            AudioStreamingResponseHandler h = ((AudioStreamingResponseHandler) handler);
            h.onAudio(new AudioChunk(audio));
        }

        // h.onThinkingToken
        String reasoningContent = delta.reasoningContent();
        if (reasoningContent != null && !reasoningContent.isEmpty() && handler instanceof ThinkingStreamingResponseHandler) {
            ThinkingStreamingResponseHandler h = ((ThinkingStreamingResponseHandler) handler);
            if (responseBuilder.compareAndSet(
                    OpenAiStreamingResponseBuilder.STATE_OUTPUT,
                    OpenAiStreamingResponseBuilder.STATE_THINKING)) {
                h.onStartThinking();
            }
            h.onThinkingToken(reasoningContent);
        }

        // h.onCompleteThinking
        // handler.onNext
        String content = delta.content();
        if (content != null && !content.isEmpty()) {
            if (responseBuilder.compareAndSet(
                    OpenAiStreamingResponseBuilder.STATE_THINKING,
                    OpenAiStreamingResponseBuilder.STATE_OUTPUT)) {
                ThinkingStreamingResponseHandler h = ((ThinkingStreamingResponseHandler) handler);
                h.onCompleteThinking(responseBuilder.build(OpenAiStreamingResponseBuilder.STATE_THINKING));
            }
            handler.onNext(content);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    private static boolean isNotBlank(String string) {
        return string != null && !string.trim().isEmpty();
    }

    public boolean isDestroy() {
        return destroy;
    }

    public void destroy() {
        if (destroy) {
            return;
        }
        OkHttpClient client = this.client;
        if (client != null) {
            destroy = true;
            try {
                client.connectionPool().evictAll();
            } catch (Exception e) {
                // ignore
            }
            Cache cache = client.cache();
            if (cache != null) {
                try {
                    cache.close();
                } catch (Exception e) {
                    // ignore
                }
            }
        }
    }

    public String modelName() {
        return modelName;
    }

    /**
     * 生成AI回复
     *
     * @param handler 回掉函数
     * @param request 供应商支持的接口参数
     * @return CompletableFuture Response AI回复，调用cancel=关闭sse链接
     */
    public CompletableFuture<Response<AiMessage>> request(StreamingResponseHandlerAdapter handler,
                                                          GenerateRequest request) {
        return request((StreamingResponseHandler) handler, request);
    }

    /**
     * 生成AI回复
     *
     * @param handler 回掉函数
     * @param request 供应商支持的接口参数
     * @return CompletableFuture Response AI回复，调用cancel=关闭sse链接
     */
    public CompletableFuture<Response<AiMessage>> request(StreamingResponseHandler<AiMessage> handler,
                                                          GenerateRequest request) {
        if (destroy) {
            throw new IllegalStateException("ai-assistant OpenAiClient(" + modelName + ") is destroy!");
        }
        List<ChatMessage> messages = request.getMessageList();
        List<ToolSpecification> toolSpecifications = request.getToolSpecificationList();

        ChatCompletionRequest.Builder builder = requestBuilder.get();
        Boolean parallelToolCalls = request.getParallelToolCalls();
        if (parallelToolCalls != null) {
            builder.parallelToolCalls(parallelToolCalls);
        }
        Double temperature = request.getTemperature();
        if (temperature != null) {
            builder.temperature(temperature);
        }
        builder.enableSearch(request.getEnableSearch());
        builder.searchOptions(request.getSearchOptions());
        builder.enableThinking(request.getEnableThinking());
        builder.thinkingBudget(request.getThinkingBudget());
        builder.modalities(request.getModalities());
        builder.audio(request.getAudio());
        builder.messages(toOpenAiMessages(messages));
        if (toolSpecifications != null && !toolSpecifications.isEmpty()) {
            if (Boolean.TRUE.equals(request.getToolChoiceRequired())) {
                ToolSpecification first = toolSpecifications.iterator().next();
                builder.toolChoice(toTool(first, strictTools));
                builder.tools(toTools(Collections.singletonList(first), strictTools));
            } else {
                builder.tools(toTools(toolSpecifications, strictTools));
            }
        }
        dev.langchain4j.model.chat.request.ResponseFormatType openAiResponseFormatType;
        JsonSchema jsonSchema;
        if (responseFormat == null) {
            openAiResponseFormatType = null;
            jsonSchema = null;
        } else if (responseFormat == ResponseFormatType.JSON_OBJECT) {
            openAiResponseFormatType = dev.langchain4j.model.chat.request.ResponseFormatType.JSON;
            jsonSchema = null;
        } else if (responseFormat == ResponseFormatType.JSON_SCHEMA) {
            openAiResponseFormatType = dev.langchain4j.model.chat.request.ResponseFormatType.JSON;
            jsonSchema = request.getJsonSchema();
        } else if (responseFormat == ResponseFormatType.TEXT) {
            openAiResponseFormatType = dev.langchain4j.model.chat.request.ResponseFormatType.TEXT;
            jsonSchema = null;
        } else {
            openAiResponseFormatType = null;
            jsonSchema = null;
        }
        if (openAiResponseFormatType != null) {
            ResponseFormat openAiResponseFormat = toOpenAiResponseFormat(dev.langchain4j.model.chat.request.ResponseFormat.builder()
                    .type(openAiResponseFormatType)
                    .jsonSchema(jsonSchema)
                    .build(), strictJsonSchema);
            builder.responseFormat(openAiResponseFormat);
        }

        ChatCompletionRequest completionRequest = builder.build();

        ChatModelRequest modelListenerRequest = null;
        Map<Object, Object> attributes = null;
        if (!listeners.isEmpty()) {
            modelListenerRequest = createModelListenerRequest(completionRequest, messages, toolSpecifications);
            attributes = new ConcurrentHashMap<>();
            ChatModelRequestContext requestContext = new ChatModelRequestContext(modelListenerRequest, attributes);
            for (ChatModelListener listener : listeners) {
                try {
                    listener.onRequest(requestContext);
                } catch (Exception e) {
                    log.warn("Exception while calling model listener", e);
                }
            }
        }

        OpenAiStreamingResponseBuilder responseBuilder = new OpenAiStreamingResponseBuilder();
        AtomicReference<String> responseId = new AtomicReference<>();
        AtomicReference<String> responseModel = new AtomicReference<>();

        Request okHttpRequest = new Request.Builder()
                .url(endpoint)
                .post(requestBody(completionRequest))
                .build();
        EventSourceCompletableFuture<Response<AiMessage>> future = new EventSourceCompletableFuture<>();
        Consumer<Throwable> errorHandler = onError(future, modelListenerRequest, attributes, responseBuilder, responseId, responseModel, handler);
        Runnable streamingCompletionCallback = onComplete(future, modelListenerRequest, attributes, responseBuilder, responseId, responseModel, handler);
        future.eventSource = EventSources.createFactory(client)
                .newEventSource(okHttpRequest, new EventSourceListener() {

                    @Override
                    public void onEvent(EventSource eventSource1, String id, String type, String data) {
                        if ("[DONE]".equals(data)) {
                            streamingCompletionCallback.run();
                            return;
                        }
                        responseBuilder.setSseData(data);
                        try {
                            ChatCompletionResponse partialResponse = objectReader.readValue(data, ChatCompletionResponse.class);
                            handle(partialResponse, handler, responseBuilder);
                            responseBuilder.append(partialResponse);
                            if (isNotBlank(partialResponse.id())) {
                                responseId.set(partialResponse.id());
                            }
                            if (isNotBlank(partialResponse.model())) {
                                responseModel.set(partialResponse.model());
                            }
                        } catch (Throwable e) {
                            errorHandler.accept(e);
                        }
                    }

                    @Override
                    public void onFailure(EventSource eventSource1, Throwable t, okhttp3.Response response) {
                        // TODO remove this when migrating from okhttp
                        if (t instanceof IllegalArgumentException && "byteCount < 0: -1".equals(t.getMessage())) {
                            streamingCompletionCallback.run();
                            return;
                        }

                        OpenAiHttpException openAiHttpException = null;
                        if (response != null) {
                            String bodyString;
                            int code = response.code();
                            try {
                                bodyString = response.body().string();
                            } catch (Exception e) {
                                bodyString = "response.body().string() fail:" + e;
                            }
                            openAiHttpException = new OpenAiHttpException(code, bodyString);
                        }
                        if (t != null) {
                            if (openAiHttpException != null) {
                                openAiHttpException.initCause(t);
                                errorHandler.accept(openAiHttpException);
                            } else {
                                errorHandler.accept(t);
                            }
                        } else if (openAiHttpException != null) {
                            errorHandler.accept(openAiHttpException);
                        } else {
                            errorHandler.accept(new OpenAiHttpException(400, "onFailure"));
                        }
                    }
                });
        return future;
    }

    private RequestBody requestBody(Object body) {
        return new RequestBody() {
            @Override
            public MediaType contentType() {
                return mediaType;
            }

            @Override
            public void writeTo(BufferedSink sink) throws IOException {
                objectWriter.writeValue(new OutputStream() {
                    @Override
                    public void write(int b) throws IOException {
                        sink.write(new byte[]{(byte) b}, 0, 1);
                    }

                    @Override
                    public void write(byte[] b, int off, int len) throws IOException {
                        sink.write(b, off, len);
                    }

                    @Override
                    public void flush() throws IOException {
                        sink.flush();
                    }
                }, body);
            }
        };
    }

    private Runnable onComplete(EventSourceCompletableFuture<Response<AiMessage>> future,
                                ChatModelRequest modelListenerRequest,
                                Map<Object, Object> attributes,
                                OpenAiStreamingResponseBuilder responseBuilder,
                                AtomicReference<String> responseId,
                                AtomicReference<String> responseModel,
                                StreamingResponseHandler<AiMessage> handler) {
        return () -> {
            Response<AiMessage> response = responseBuilder.build(OpenAiStreamingResponseBuilder.STATE_OUTPUT);
            try {
                future.complete(response);
            } catch (Throwable t) {
                log.warn("Exception while calling model onComplete EventSourceCompletableFuture {}", t.toString(), t);
            }
            if (handler instanceof ThinkingStreamingResponseHandler && response.content() instanceof ThinkingAiMessage) {
                ThinkingStreamingResponseHandler h = ((ThinkingStreamingResponseHandler) handler);
                try {
                    h.onCompleteThinking(response);
                } catch (Throwable t) {
                    log.warn("Exception while calling model onCompleteThinking {}", t.toString(), t);
                }
            }
            if (!listeners.isEmpty() && modelListenerRequest != null) {
                ChatModelResponse modelListenerResponse = createModelListenerResponse(
                        responseId.get(),
                        responseModel.get(),
                        response
                );
                ChatModelResponseContext responseContext = new ChatModelResponseContext(
                        modelListenerResponse,
                        modelListenerRequest,
                        attributes
                );
                for (ChatModelListener listener : listeners) {
                    try {
                        listener.onResponse(responseContext);
                    } catch (Exception e) {
                        log.warn("Exception while calling model listener", e);
                    }
                }
            }
            handler.onComplete(response);
        };
    }

    private Consumer<Throwable> onError(
            EventSourceCompletableFuture<Response<AiMessage>> future,
            ChatModelRequest modelListenerRequest,
            Map<Object, Object> attributes,
            OpenAiStreamingResponseBuilder responseBuilder,
            AtomicReference<String> responseId,
            AtomicReference<String> responseModel,
            StreamingResponseHandler<AiMessage> handler
    ) {
        return error -> {
            try {
                future.completeExceptionally(error);
            } catch (Throwable t) {
                log.warn("Exception while calling model onError EventSourceCompletableFuture {}", t.toString(), t);
            }
            Response<AiMessage> response = responseBuilder.build(OpenAiStreamingResponseBuilder.STATE_OUTPUT);

            if (!listeners.isEmpty() && modelListenerRequest != null) {
                ChatModelResponse modelListenerPartialResponse = createModelListenerResponse(
                        responseId.get(),
                        responseModel.get(),
                        response
                );
                ChatModelErrorContext errorContext = new ChatModelErrorContext(
                        error,
                        modelListenerRequest,
                        modelListenerPartialResponse,
                        attributes
                );
                for (ChatModelListener listener : listeners) {
                    try {
                        listener.onError(errorContext);
                    } catch (Exception e) {
                        log.warn("Exception while calling model listener", e);
                    }
                }
            }

            handler.onError(error);
        };
    }

    @Override
    public String toString() {
        return "OpenAiChatClient{" +
                "modelName='" + modelName + '\'' +
                ", responseFormat=" + responseFormat +
                ", endpoint='" + endpoint + '\'' +
                ", destroy=" + destroy +
                '}';
    }

    private static class EventSourceCompletableFuture<T> extends CompletableFuture<T> {
        private EventSource eventSource;

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            if (isCancelled()) {
                return false;
            }
            EventSource eventSource = this.eventSource;
            if (eventSource != null) {
                eventSource.cancel();
            }
            return super.cancel(mayInterruptIfRunning);
        }
    }

    public static class Builder {
        private String baseUrl;
        private String apiKey;
        private String modelName;
        private Double temperature;
        private Double topP;
        private List<String> stop;
        private Integer maxCompletionTokens;
        private Double presencePenalty;
        private Double frequencyPenalty;
        private Map<String, Integer> logitBias;
        private String responseFormat;
        private Integer seed;
        private String user;
        private Boolean strictTools;
        private Boolean parallelToolCalls;
        private Duration timeout;
        private Duration connectTimeout;
        private Proxy proxy;
        private Integer n;
        private Map<String, String> customHeaders;
        private List<ChatModelListener> listeners;
        private Boolean strictJsonSchema;

        public Builder() {
        }

        public Builder strictJsonSchema(Boolean strictJsonSchema) {
            this.strictJsonSchema = strictJsonSchema;
            return this;
        }

        public Builder modelName(String modelName) {
            this.modelName = modelName;
            return this;
        }

        public Builder baseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        public Builder n(Integer n) {
            this.n = n;
            return this;
        }

        public Builder apiKey(String apiKey) {
            this.apiKey = apiKey;
            return this;
        }

        public Builder temperature(Double temperature) {
            this.temperature = temperature;
            return this;
        }

        public Builder topP(Double topP) {
            this.topP = topP;
            return this;
        }

        public Builder stop(List<String> stop) {
            this.stop = stop;
            return this;
        }

        public Builder maxCompletionTokens(Integer maxCompletionTokens) {
            this.maxCompletionTokens = maxCompletionTokens;
            return this;
        }

        public Builder presencePenalty(Double presencePenalty) {
            this.presencePenalty = presencePenalty;
            return this;
        }

        public Builder frequencyPenalty(Double frequencyPenalty) {
            this.frequencyPenalty = frequencyPenalty;
            return this;
        }

        public Builder logitBias(Map<String, Integer> logitBias) {
            this.logitBias = logitBias;
            return this;
        }

        public Builder responseFormat(String responseFormat) {
            this.responseFormat = responseFormat;
            return this;
        }

        public Builder seed(Integer seed) {
            this.seed = seed;
            return this;
        }

        public Builder user(String user) {
            this.user = user;
            return this;
        }

        public Builder strictTools(Boolean strictTools) {
            this.strictTools = strictTools;
            return this;
        }

        public Builder parallelToolCalls(Boolean parallelToolCalls) {
            this.parallelToolCalls = parallelToolCalls;
            return this;
        }

        public Builder timeout(Duration timeout) {
            this.timeout = timeout;
            return this;
        }

        public Builder connectTimeout(Duration connectTimeout) {
            this.connectTimeout = connectTimeout;
            return this;
        }

        public Builder proxy(Proxy proxy) {
            this.proxy = proxy;
            return this;
        }

        public Builder customHeaders(Map<String, String> customHeaders) {
            this.customHeaders = customHeaders;
            return this;
        }

        public Builder listeners(List<ChatModelListener> listeners) {
            this.listeners = listeners;
            return this;
        }

        public OpenAiChatClient build() {
            return new OpenAiChatClient(this.baseUrl, this.apiKey,
                    this.modelName, this.temperature, this.topP, this.stop, this.maxCompletionTokens,
                    this.presencePenalty, this.frequencyPenalty, this.logitBias, this.responseFormat,
                    this.seed, this.user, this.strictTools, this.parallelToolCalls,
                    this.timeout, this.connectTimeout, this.proxy,
                    this.customHeaders, this.n, this.listeners, this.strictJsonSchema);
        }
    }

    public static class Cancellable {

        final EventSource eventSource;

        public Cancellable(EventSource eventSource) {
            this.eventSource = eventSource;
        }

        public void cancel() {
            eventSource.cancel();
        }
    }
}
