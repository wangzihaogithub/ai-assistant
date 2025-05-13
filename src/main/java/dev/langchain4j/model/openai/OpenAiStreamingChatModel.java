package dev.langchain4j.model.openai;

import com.github.aiassistant.entity.model.langchain4j.ThinkingAiMessage;
import com.github.aiassistant.service.text.GenerateRequest;
import dev.ai4j.openai4j.OpenAiClient;
import dev.ai4j.openai4j.ResponseHandle;
import dev.ai4j.openai4j.chat.*;
import dev.ai4j.openai4j.shared.StreamOptions;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.StreamingResponseHandler;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.chat.listener.*;
import dev.langchain4j.model.chat.request.json.JsonSchema;
import dev.langchain4j.model.openai.spi.OpenAiStreamingChatModelBuilderFactory;
import dev.langchain4j.model.output.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.Proxy;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static dev.langchain4j.internal.Utils.getOrDefault;
import static dev.langchain4j.internal.Utils.isNullOrBlank;
import static dev.langchain4j.model.openai.InternalOpenAiHelper.*;
import static dev.langchain4j.spi.ServiceHelper.loadFactories;
import static java.time.Duration.ofSeconds;
import static java.util.Collections.emptyList;

/**
 * 通义千问API接口文档：https://help.aliyun.com/zh/model-studio/use-qwen-by-calling-api
 * Represents an OpenAI language model with a chat completion interface, such as gpt-3.5-turbo and gpt-4.
 * The model's response is streamed token by token and should be handled with {@link StreamingResponseHandler}.
 * You can find description of parameters <a href="https://platform.openai.com/docs/api-reference/chat/create">here</a>.
 */
public class OpenAiStreamingChatModel implements StreamingChatLanguageModel {
    private static final Logger log = LoggerFactory.getLogger(OpenAiStreamingChatModel.class);
    private final OpenAiClient client;
    private final Boolean strictTools;
    private final Boolean strictJsonSchema;
    private final String modelName;
    private final ResponseFormatType responseFormat;
    private final List<ChatModelListener> listeners;
    private final Supplier<ChatCompletionRequest.Builder> requestBuilder;

    public OpenAiStreamingChatModel(String baseUrl,
                                    String apiKey,
                                    String organizationId,
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
                                    Proxy proxy,
                                    Boolean logRequests,
                                    Boolean logResponses,
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

        timeout = getOrDefault(timeout, ofSeconds(60));

        this.client = OpenAiClient.builder()
                .baseUrl(getOrDefault(baseUrl, OPENAI_URL))
                .openAiApiKey(apiKey)
                .organizationId(organizationId)
                .callTimeout(timeout)
                .connectTimeout(timeout)
                .readTimeout(timeout)
                .writeTimeout(timeout)
                .proxy(proxy)
                .logRequests(logRequests)
                .logStreamingResponses(logResponses)
                .userAgent(DEFAULT_USER_AGENT)
                .customHeaders(customHeaders)
                .build();
        this.modelName = modelName;
        this.responseFormat = ResponseFormatType.valueOf(responseFormat.toUpperCase(Locale.ROOT));
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

    public static OpenAiStreamingChatModelBuilder builder() {
        for (OpenAiStreamingChatModelBuilderFactory factory : loadFactories(OpenAiStreamingChatModelBuilderFactory.class)) {
            return factory.get();
        }
        return new OpenAiStreamingChatModelBuilder();
    }

    public String modelName() {
        return modelName;
    }

    @Override
    public void generate(List<ChatMessage> messages, StreamingResponseHandler<AiMessage> handler) {
        throw new IllegalArgumentException("Tools are currently not supported by this model");
    }

    /**
     * 生成AI回复
     *
     * @param handler 回掉函数
     * @param request 供应商支持的接口参数
     * @return ResponseHandle 客户端取消处理
     */
    public ResponseHandle request(StreamingResponseHandler<AiMessage> handler,
                                  GenerateRequest request) {
        List<ChatMessage> messages = request.getMessageList();
        List<ToolSpecification> toolSpecifications = request.getToolSpecificationList();

        ChatCompletionRequest.Builder builder = requestBuilder.get();
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
        ResponseFormat openAiResponseFormat = toOpenAiResponseFormat(dev.langchain4j.model.chat.request.ResponseFormat.builder()
                .type(openAiResponseFormatType)
                .jsonSchema(jsonSchema)
                .build(), strictJsonSchema);
        builder.responseFormat(openAiResponseFormat);

        ChatCompletionRequest completionRequest = builder.build();

        ChatModelRequest modelListenerRequest = createModelListenerRequest(completionRequest, messages, toolSpecifications);
        Map<Object, Object> attributes = new ConcurrentHashMap<>();
        ChatModelRequestContext requestContext = new ChatModelRequestContext(modelListenerRequest, attributes);
        listeners.forEach(listener -> {
            try {
                listener.onRequest(requestContext);
            } catch (Exception e) {
                log.warn("Exception while calling model listener", e);
            }
        });

        OpenAiStreamingResponseBuilder responseBuilder = new OpenAiStreamingResponseBuilder();

        AtomicReference<String> responseId = new AtomicReference<>();
        AtomicReference<String> responseModel = new AtomicReference<>();
        return client.chatCompletion(completionRequest)
                .onPartialResponse(partialResponse -> {
                    handle(partialResponse, handler, responseBuilder);
                    responseBuilder.append(partialResponse);
                    if (!isNullOrBlank(partialResponse.id())) {
                        responseId.set(partialResponse.id());
                    }
                    if (!isNullOrBlank(partialResponse.model())) {
                        responseModel.set(partialResponse.model());
                    }
                })
                .onComplete(() -> {
                    Response<AiMessage> response = responseBuilder.build(OpenAiStreamingResponseBuilder.STATE_OUTPUT);
                    if (handler instanceof ThinkingStreamingResponseHandler && response.content() instanceof ThinkingAiMessage) {
                        ThinkingStreamingResponseHandler h = ((ThinkingStreamingResponseHandler) handler);
                        h.onCompleteThinking(response);
                    }
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
                    listeners.forEach(listener -> {
                        try {
                            listener.onResponse(responseContext);
                        } catch (Exception e) {
                            log.warn("Exception while calling model listener", e);
                        }
                    });

                    handler.onComplete(response);
                })
                .onError(error -> {
                    Response<AiMessage> response = responseBuilder.build(OpenAiStreamingResponseBuilder.STATE_OUTPUT);

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

                    listeners.forEach(listener -> {
                        try {
                            listener.onError(errorContext);
                        } catch (Exception e) {
                            log.warn("Exception while calling model listener", e);
                        }
                    });

                    handler.onError(error);
                })
                .execute();
    }

    public static class OpenAiStreamingChatModelBuilder {
        private String baseUrl;
        private String apiKey;
        private String organizationId;
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
        private Proxy proxy;
        private Integer n;
        private Boolean logRequests;
        private Boolean logResponses;
        private Map<String, String> customHeaders;
        private List<ChatModelListener> listeners;
        private Boolean strictJsonSchema;

        public OpenAiStreamingChatModelBuilder() {
        }

        public OpenAiStreamingChatModelBuilder strictJsonSchema(Boolean strictJsonSchema) {
            this.strictJsonSchema = strictJsonSchema;
            return this;
        }

        public OpenAiStreamingChatModelBuilder modelName(String modelName) {
            this.modelName = modelName;
            return this;
        }

        public OpenAiStreamingChatModelBuilder modelName(OpenAiChatModelName modelName) {
            this.modelName = modelName.toString();
            return this;
        }

        public OpenAiStreamingChatModelBuilder baseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        public OpenAiStreamingChatModelBuilder n(Integer n) {
            this.n = n;
            return this;
        }

        public OpenAiStreamingChatModelBuilder apiKey(String apiKey) {
            this.apiKey = apiKey;
            return this;
        }

        public OpenAiStreamingChatModelBuilder organizationId(String organizationId) {
            this.organizationId = organizationId;
            return this;
        }

        public OpenAiStreamingChatModelBuilder temperature(Double temperature) {
            this.temperature = temperature;
            return this;
        }

        public OpenAiStreamingChatModelBuilder topP(Double topP) {
            this.topP = topP;
            return this;
        }

        public OpenAiStreamingChatModelBuilder stop(List<String> stop) {
            this.stop = stop;
            return this;
        }

        public OpenAiStreamingChatModelBuilder maxCompletionTokens(Integer maxCompletionTokens) {
            this.maxCompletionTokens = maxCompletionTokens;
            return this;
        }

        public OpenAiStreamingChatModelBuilder presencePenalty(Double presencePenalty) {
            this.presencePenalty = presencePenalty;
            return this;
        }

        public OpenAiStreamingChatModelBuilder frequencyPenalty(Double frequencyPenalty) {
            this.frequencyPenalty = frequencyPenalty;
            return this;
        }

        public OpenAiStreamingChatModelBuilder logitBias(Map<String, Integer> logitBias) {
            this.logitBias = logitBias;
            return this;
        }

        public OpenAiStreamingChatModelBuilder responseFormat(String responseFormat) {
            this.responseFormat = responseFormat;
            return this;
        }

        public OpenAiStreamingChatModelBuilder seed(Integer seed) {
            this.seed = seed;
            return this;
        }

        public OpenAiStreamingChatModelBuilder user(String user) {
            this.user = user;
            return this;
        }

        public OpenAiStreamingChatModelBuilder strictTools(Boolean strictTools) {
            this.strictTools = strictTools;
            return this;
        }

        public OpenAiStreamingChatModelBuilder parallelToolCalls(Boolean parallelToolCalls) {
            this.parallelToolCalls = parallelToolCalls;
            return this;
        }

        public OpenAiStreamingChatModelBuilder timeout(Duration timeout) {
            this.timeout = timeout;
            return this;
        }

        public OpenAiStreamingChatModelBuilder proxy(Proxy proxy) {
            this.proxy = proxy;
            return this;
        }

        public OpenAiStreamingChatModelBuilder logRequests(Boolean logRequests) {
            this.logRequests = logRequests;
            return this;
        }

        public OpenAiStreamingChatModelBuilder logResponses(Boolean logResponses) {
            this.logResponses = logResponses;
            return this;
        }

        public OpenAiStreamingChatModelBuilder customHeaders(Map<String, String> customHeaders) {
            this.customHeaders = customHeaders;
            return this;
        }

        public OpenAiStreamingChatModelBuilder listeners(List<ChatModelListener> listeners) {
            this.listeners = listeners;
            return this;
        }

        public OpenAiStreamingChatModel build() {
            return new OpenAiStreamingChatModel(this.baseUrl, this.apiKey, this.organizationId,
                    this.modelName, this.temperature, this.topP, this.stop, this.maxCompletionTokens,
                    this.presencePenalty, this.frequencyPenalty, this.logitBias, this.responseFormat,
                    this.seed, this.user, this.strictTools, this.parallelToolCalls,
                    this.timeout, this.proxy, this.logRequests, this.logResponses,
                    this.customHeaders, this.n, this.listeners, this.strictJsonSchema);
        }
    }
}
