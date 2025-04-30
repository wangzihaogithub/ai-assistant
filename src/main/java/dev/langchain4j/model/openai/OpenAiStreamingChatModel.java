package dev.langchain4j.model.openai;

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
import dev.langchain4j.model.openai.spi.OpenAiStreamingChatModelBuilderFactory;
import dev.langchain4j.model.output.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.Proxy;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
    private final String modelName;
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
                                    List<ChatModelListener> listeners) {

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
        this.strictTools = strictTools;
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
                .responseFormat(responseFormat == null ? null : ResponseFormat.builder()
                        .type(ResponseFormatType.valueOf(responseFormat.toUpperCase(Locale.ROOT)))
                        .build())
                .seed(seed)
                .user(user)
                .parallelToolCalls(parallelToolCalls);
    }

    private static void handle(ChatCompletionResponse partialResponse,
                               StreamingResponseHandler<AiMessage> handler) {
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
        String reasoningContent = delta.reasoningContent();
        if (reasoningContent != null && handler instanceof ThinkingStreamingResponseHandler) {
            ((ThinkingStreamingResponseHandler<AiMessage>) handler).onThinkingToken(reasoningContent);
        }
        String content = delta.content();
        if (content != null) {
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
     * @param messages           由历史对话组成的消息列表。
     * @param toolSpecifications 可供模型调用的工具数组，可以包含一个或多个工具对象。一次Function Calling流程模型会从中选择一个工具。
     *                           目前不支持通义千问VL/Audio，也不建议用于数学和代码模型。
     * @param handler            回掉函数
     * @param enableSearch       # 开启联网搜索的参数
     * @param searchOptions      search_options = {
     *                           "forced_search": True, # 强制开启联网搜索
     *                           "enable_source": True, # 使返回结果包含搜索来源的信息，OpenAI 兼容方式暂不支持返回
     *                           "enable_citation": True, # 开启角标标注功能
     *                           "citation_format": "[ref_<number>]", # 角标形式为[ref_i]
     *                           "search_strategy": "pro" # 模型将搜索10条互联网信息
     *                           },
     * @param enableThinking     # 是否开启思考模式，适用于 Qwen3 模型。
     *                           Qwen3 商业版模型默认值为 False，Qwen3 开源版模型默认值为 True。
     * @param thinkingBudget     参数设置最大推理过程 Token 数，不能超过供应商要求的最大思维链Token数：例：38912。两个参数对 QwQ 与 DeepSeek-R1 模型无效
     * @param modalities         （可选）默认值为["text"]
     *                           输出数据的模态，仅支持 Qwen-Omni 模型指定。可选值：
     *                           ["text","audio"]：输出文本与音频；
     *                           ["text"]：输出文本。
     * @return ResponseHandle 取消逻辑为客户端取消
     */
    public ResponseHandle generateV2(List<ChatMessage> messages,
                                     List<ToolSpecification> toolSpecifications,
                                     StreamingResponseHandler<AiMessage> handler,
                                     Boolean enableSearch,
                                     Map<String, Object> searchOptions,
                                     Boolean enableThinking,
                                     Integer thinkingBudget,
                                     List<String> modalities) {
        ChatCompletionRequest.Builder builder = requestBuilder.get();
        builder.enableSearch(enableSearch);
        builder.searchOptions(searchOptions);
        builder.enableThinking(enableThinking);
        builder.thinkingBudget(thinkingBudget);
        builder.modalities(modalities);
        builder.messages(toOpenAiMessages(messages));
        if (toolSpecifications != null && !toolSpecifications.isEmpty()) {
            builder.tools(toTools(toolSpecifications, strictTools));
        }

        ChatCompletionRequest request = builder.build();

        ChatModelRequest modelListenerRequest = createModelListenerRequest(request, messages, toolSpecifications);
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

        return client.chatCompletion(request)
                .onPartialResponse(partialResponse -> {
                    responseBuilder.append(partialResponse);
                    handle(partialResponse, handler);

                    if (!isNullOrBlank(partialResponse.id())) {
                        responseId.set(partialResponse.id());
                    }
                    if (!isNullOrBlank(partialResponse.model())) {
                        responseModel.set(partialResponse.model());
                    }
                })
                .onComplete(() -> {
                    Response<AiMessage> response = responseBuilder.build();

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
                    Response<AiMessage> response = responseBuilder.build();

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

        public OpenAiStreamingChatModelBuilder() {
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
                    this.customHeaders, this.n, this.listeners);
        }
    }
}
