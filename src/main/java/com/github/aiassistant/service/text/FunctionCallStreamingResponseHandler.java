package com.github.aiassistant.service.text;

import com.github.aiassistant.entity.model.chat.LangChainUserMessage;
import com.github.aiassistant.entity.model.chat.MemoryIdVO;
import com.github.aiassistant.entity.model.chat.MetadataAiMessage;
import com.github.aiassistant.entity.model.chat.QuestionClassifyListVO;
import com.github.aiassistant.exception.TokenReadTimeoutException;
import com.github.aiassistant.service.jsonschema.LlmJsonSchemaApiService;
import com.github.aiassistant.service.jsonschema.WhetherWaitingForAiJsonSchema;
import com.github.aiassistant.service.text.sseemitter.AiMessageString;
import com.github.aiassistant.service.text.sseemitter.SseHttpResponse;
import com.github.aiassistant.service.text.tools.ResultToolExecutor;
import com.github.aiassistant.service.text.tools.Tools;
import com.github.aiassistant.util.AiUtil;
import com.github.aiassistant.util.FutureUtil;
import com.github.aiassistant.util.StringUtils;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.model.openai.ThinkingStreamingResponseHandler;
import dev.langchain4j.model.output.FinishReason;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.output.TokenUsage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/**
 * 流式处理AI结果（同时调用工具库）
 */
public class FunctionCallStreamingResponseHandler extends CompletableFuture<Void> implements ThinkingStreamingResponseHandler<AiMessage> {
    public static final long READ_DONE = -1L;
    private static final ScheduledThreadPoolExecutor READ_TIMEOUT_SCHEDULED = new ScheduledThreadPoolExecutor(1, new ThreadFactory() {

        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r);
            thread.setName("ai-readTimeout" + thread.getId());
            thread.setDaemon(true);
            return thread;
        }
    });
    private static final Logger log = LoggerFactory.getLogger(FunctionCallStreamingResponseHandler.class);
    public static int MAX_GENERATE_COUNT = 10;
    private final Object memoryId;
    private final ChatMemory chatMemory;
    private final String modelName;
    private final OpenAiStreamingChatModel chatModel;
    private final List<Tools> tools;
    private final List<Tools.ToolMethod> toolMethodList;
    private final FunctionCallStreamingResponseHandler parent;
    private final ChatStreamingResponseHandler bizHandler;
    private final int baseMessageIndex;
    private final AtomicInteger addMessageCount;
    /**
     * JsonSchema类型的模型
     */
    private final LlmJsonSchemaApiService llmJsonSchemaApiService;
    private final Executor executor;
    private final int generateRemaining;
    private final List<SseHttpResponseImpl> aiEmitterList = new ArrayList<>();
    private final boolean isSupportChineseToolName;
    private final Long readTimeoutMs;
    private final QuestionClassifyListVO classifyListVO;
    private final Boolean websearch;
    private final Boolean reasoning;
    private Tools.ParamValidationResult validationResult;
    private Response<AiMessage> lastResponse;
    private volatile long lastReadTimestamp;
    private volatile ScheduledFuture<?> readTimeoutFuture;
    private Boolean enableSearch;
    private Map<String, Object> searchOptions;
    private Boolean enableThinking;
    private Integer thinkingBudget;

    public FunctionCallStreamingResponseHandler(String modelName, OpenAiStreamingChatModel chatModel,
                                                ChatMemory chatMemory,
                                                ChatStreamingResponseHandler handler,
                                                LlmJsonSchemaApiService llmJsonSchemaApiService,
                                                List<Tools.ToolMethod> toolMethodList,
                                                boolean isSupportChineseToolName,
                                                int baseMessageIndex,
                                                int addMessageCount,
                                                Long readTimeoutMs,
                                                QuestionClassifyListVO classifyListVO,
                                                Boolean websearch,
                                                Boolean reasoning,
                                                Executor executor) {
        this.readTimeoutMs = readTimeoutMs;
        this.modelName = modelName;
        this.tools = toolMethodList.stream().map(Tools.ToolMethod::tool).collect(Collectors.toList());
        for (Tools tool : tools) {
            tool.setStreamingResponseHandler(handler);
        }
        this.isSupportChineseToolName = isSupportChineseToolName;
        this.toolMethodList = toolMethodList;
        this.executor = executor == null ? Runnable::run : executor;
        this.memoryId = chatMemory.id();
        this.llmJsonSchemaApiService = llmJsonSchemaApiService;
        this.chatModel = chatModel;
        this.chatMemory = chatMemory;
        this.parent = null;
        this.bizHandler = handler;
        this.baseMessageIndex = baseMessageIndex;
        this.classifyListVO = classifyListVO;
        this.websearch = websearch;
        this.reasoning = reasoning;
        this.addMessageCount = new AtomicInteger(addMessageCount);
        this.generateRemaining = MAX_GENERATE_COUNT;
    }

    protected FunctionCallStreamingResponseHandler(FunctionCallStreamingResponseHandler parent) {
        this.readTimeoutMs = parent.readTimeoutMs;
        this.modelName = parent.modelName;
        this.memoryId = parent.memoryId;
        this.tools = parent.tools;
        this.toolMethodList = parent.toolMethodList;
        this.isSupportChineseToolName = parent.isSupportChineseToolName;
        this.executor = parent.executor;
        this.chatModel = parent.chatModel;
        this.chatMemory = parent.chatMemory;
        this.parent = parent;
        this.bizHandler = parent.bizHandler;
        this.baseMessageIndex = parent.baseMessageIndex;
        this.addMessageCount = parent.addMessageCount;
        this.llmJsonSchemaApiService = parent.llmJsonSchemaApiService;
        this.classifyListVO = parent.classifyListVO;
        this.websearch = parent.websearch;
        this.reasoning = parent.reasoning;
        this.generateRemaining = parent.generateRemaining - 1;
        this.validationResult = parent.validationResult;
        this.lastResponse = parent.lastResponse;
        this.enableSearch = parent.enableSearch;
        this.enableThinking = parent.enableThinking;
        this.thinkingBudget = parent.thinkingBudget;
        this.searchOptions = parent.searchOptions;
    }

    protected FunctionCallStreamingResponseHandler fork(FunctionCallStreamingResponseHandler parent) {
        return new FunctionCallStreamingResponseHandler(parent);
    }

    /**
     * langchain4j onError callback
     * This method is invoked when an error occurs during streaming.
     *
     * @param error The error that occurred
     */
    @Override
    public void onError(Throwable error) {
        Objects.requireNonNull(error, "throwable cannot be null");
        Throwable rootError = error instanceof CompletionException ? error.getCause() : error;
        ExecutionException exception = new ExecutionException(
                "llm error! modelName=" + modelName + " " + rootError.getMessage(),
                rootError);
        exceptionally(exception);
    }

    /**
     * langchain4j onComplete callback
     * Invoked when the language model has finished streaming a response.
     * If the model executes one or multiple tools, it is accessible via {@link dev.langchain4j.data.message.AiMessage#toolExecutionRequests()}.
     *
     * @param response The complete response generated by the language model.
     *                 For textual responses, it contains all tokens from {@link #onNext} concatenated.
     */
    @Override
    public void onComplete(Response<AiMessage> response) {
        this.lastReadTimestamp = READ_DONE;
        this.lastResponse = response;
        try {
            if (AiUtil.isErrorAiToolMessage(response)) {
                response = AiUtil.filterErrorToolRequestId(response);
            }
            onComplete0(response);
        } catch (Exception e) {
            onError(e);
        }
    }

    @Override
    public void onStartThinking() {
        try {
            bizHandler.onBeforeModelThinking();
        } catch (Exception e) {
            log.error("onBeforeThinking failed {}", e.toString(), e);
        }
    }

    /**
     * 思考模型独有的
     *
     * @param thinkingToken 思考内容
     */
    @Override
    public void onThinkingToken(String thinkingToken) {
        try {
            bizHandler.onModelThinkingToken(thinkingToken);
        } catch (Exception e) {
            log.error("onThinkingToken {} failed {}", e.toString(), thinkingToken, e);
        }
    }

    @Override
    public void onCompleteThinking(Response<AiMessage> thinkingResponse) {
        try {
            bizHandler.onAfterModelThinking(thinkingResponse);
        } catch (Exception e) {
            log.error("onAfterThinking failed {}. {}", e.toString(), thinkingResponse, e);
        }
    }

    /**
     * langchain4j onNext callback
     * Invoked each time the language model generates a new token in a textual response.
     * If the model executes a tool instead, this method will not be invoked; {@link #onComplete} will be invoked instead.
     *
     * @param token The newly generated token, which is a part of the complete response.
     */
    @Override
    public void onNext(String token) {
        onNext(new AiMessageString(token));
    }

    private void onNext(AiMessageString token) {
        if (readTimeoutFuture == null && readTimeoutMs != null) {
            synchronized (this) {
                if (readTimeoutFuture == null) {
                    this.readTimeoutFuture = READ_TIMEOUT_SCHEDULED.scheduleWithFixedDelay(this::timeoutCheck, readTimeoutMs, Math.max(readTimeoutMs / 2, 500), TimeUnit.MILLISECONDS);
                }
            }
        }
        this.lastReadTimestamp = System.currentTimeMillis();
        Tools.ParamValidationResult validationResult = this.validationResult;
        if (validationResult != null) {
            appendAiMessage(validationResult.getAiMessage());
        }
        try {
            bizHandler.onToken(token, baseMessageIndex, addMessageCount.get());
        } catch (Exception e) {
            onError(e);
        }
    }

    private void timeoutCheck() {
        long lastReadTimestamp = this.lastReadTimestamp;
        if (lastReadTimestamp == READ_DONE) {
            readTimeoutFuture.cancel(false);
        } else if ((System.currentTimeMillis() - lastReadTimestamp) >= readTimeoutMs) {
            readTimeoutFuture.cancel(false);
            long timeout = System.currentTimeMillis() - lastReadTimestamp;
            TokenReadTimeoutException exception = new TokenReadTimeoutException(String.format("llm error! tokenReadTimeout modelName '%s' readTimeout %s/ms, config '%sms'", modelName, timeout, readTimeoutMs), timeout, readTimeoutMs);
            exceptionally(exception);
        }
    }

    private void appendAiMessage(AiMessage message) {
        Response<AiMessage> validationResponse = new Response<>(message, lastResponse.tokenUsage(), lastResponse.finishReason(), lastResponse.metadata());
        this.validationResult = null;
        onNext(message.text());
        onTokenEnd(validationResponse);
        bizHandler.onTokenBegin(baseMessageIndex, addMessageCount.get(), generateCount());
    }

    private void onComplete0(Response<AiMessage> response) {
        AiMessage aiMessage = response.content();
        if (aiMessage.hasToolExecutionRequests()) {
            List<ResultToolExecutor> toolExecutors = buildToolExecutor(aiMessage.toolExecutionRequests(), response);
            executeToolsValidationAndCall(toolExecutors, response).whenComplete((validationFail, throwable) -> {
                if (throwable != null) {
                    onError(throwable);
                } else if (validationFail != null) {
                    done(validationFail);
                } else {
                    boolean isEmitter = toolExecutors.stream().allMatch(ResultToolExecutor::isEmitter);
                    List<ToolExecutionResultMessage> resultMessageList = toolExecutors.stream()
                            .map(e -> e.getNow(null))
                            .collect(Collectors.toList());
                    for (ToolExecutionResultMessage tm : resultMessageList) {
                        addMessage(tm);
                    }
                    for (SseHttpResponseImpl emitter : aiEmitterList) {
                        emitter.ready();
                    }
                    if (!isEmitter || aiEmitterList.stream().allMatch(SseHttpResponseImpl::isEmpty)) {
                        generate();
                    }
                }
            });
        } else {
            // 确认是否需要工具调用
            isNeedConfirmToolCall(response).whenComplete((b, throwable) -> {
                if (Boolean.TRUE.equals(b)) {
                    onTokenEnd(response);
                    bizHandler.onTokenBegin(baseMessageIndex, addMessageCount.get(), generateCount());
                    // 这里自动帮用户确认，不计聊天记录
                    addMessage(new LangChainUserMessage("好"));
                    generate();
                } else {
                    onTokenEnd(response);
                    done(response);
                }
            });
        }
    }

    private CompletableFuture<Response<AiMessage>> executeToolsValidationAndCall(List<ResultToolExecutor> toolExecutors, Response<AiMessage> response) {
        CompletableFuture<Response<AiMessage>> future = new CompletableFuture<>();
        CompletableFuture<Tools.ParamValidationResult> validationResultFuture = executeToolsValidation(toolExecutors);
        validationResultFuture.whenComplete((validationResult, throwable) -> {
            this.validationResult = validationResult;
            try {
                if (throwable != null) {
                    // 验证代码报错了
                    future.completeExceptionally(throwable);
                } else if (validationResult != null && validationResult.isStopExecute()) {
                    // 参数验证不通过，不能调用工具
                    AiMessage validationAiMessage = validationResult.getAiMessage();
                    Response<AiMessage> validationResponse = new Response<>(validationAiMessage, response.tokenUsage(), response.finishReason(), response.metadata());
                    onTokenEnd(validationResponse);
                    future.complete(validationResponse);
                } else {
                    // 调用工具
                    onTokenEnd(response);
                    bizHandler.onTokenBegin(baseMessageIndex, addMessageCount.get(), generateCount());
                    bizHandler.onToolCalls(response);
                    executor.execute(() -> {
                        try {
                            executeToolsCall(toolExecutors, future);
                        } catch (Throwable e) {
                            future.completeExceptionally(e);
                        }
                    });
                }
            } catch (Throwable e) {
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    private void executeToolsCall(List<ResultToolExecutor> toolExecutors, CompletableFuture<Response<AiMessage>> future) {
        // 执行
        for (ResultToolExecutor executor : toolExecutors) {
            executor.execute();
            executor.whenComplete(new BiConsumer<ToolExecutionResultMessage, Throwable>() {
                @Override
                public void accept(ToolExecutionResultMessage toolExecutionResultMessage, Throwable throwable) {
                    bizHandler.onAfterToolCalls(executor.getRequest(), toolExecutionResultMessage, throwable);
                }
            });
        }
        // 全部执行完成后，
        FutureUtil.allOf(toolExecutors).whenComplete((unused, throwable1) -> {
            if (throwable1 != null) {
                future.completeExceptionally(throwable1);
            } else {
                try {
                    future.complete(null);
                } catch (Throwable e) {
                    future.completeExceptionally(e);
                }
            }
        });
    }

    private CompletableFuture<Boolean> isNeedConfirmToolCall(Response<AiMessage> response) {
        if (generateRemaining <= 0) {
            return CompletableFuture.completedFuture(false);
        }
        if (tools.isEmpty()) {
            return CompletableFuture.completedFuture(false);
        }
        String aiText = response.content().text();
        if (!StringUtils.hasText(aiText)) {
            return CompletableFuture.completedFuture(false);
        }
//        if (AiUtil.isNeedConfirmToolCall(response, toolMethodList)) {
//            return CompletableFuture.completedFuture(true);
//        }
        if (memoryId instanceof MemoryIdVO && llmJsonSchemaApiService != null) {
            WhetherWaitingForAiJsonSchema schema = llmJsonSchemaApiService.getWhetherWaitingForAiJsonSchema((MemoryIdVO) memoryId);
            if (schema == null) {
                return CompletableFuture.completedFuture(false);
            }
            return schema.future(aiText);
        } else {
            return CompletableFuture.completedFuture(false);
        }
    }

    private void done(Response<AiMessage> response) {
        bizHandler.onComplete(response, baseMessageIndex, addMessageCount.get(), generateCount());
        FunctionCallStreamingResponseHandler handler = this;
        while (handler != null) {
            handler.complete(null);
            handler = handler.parent;
        }
    }

    private void onTokenEnd(Response<AiMessage> response) {
        addMessage(MetadataAiMessage.convert(response));
        bizHandler.onTokenEnd(response, baseMessageIndex, addMessageCount.get(), generateCount());
    }

    public void addMessage(ChatMessage message) {
        chatMemory.add(message);
        addMessageCount.incrementAndGet();
    }

    /**
     * 生成AI回复
     *
     * @return 回复
     */
    public CompletableFuture<Void> generate() {
        return generate(enableSearch, searchOptions, enableThinking, thinkingBudget);
    }

    /**
     * 生成AI回复
     *
     * @param enableSearch   # 开启联网搜索的参数
     * @param searchOptions  search_options = {
     *                       "forced_search": True, # 强制开启联网搜索
     *                       "enable_source": True, # 使返回结果包含搜索来源的信息，OpenAI 兼容方式暂不支持返回
     *                       "enable_citation": True, # 开启角标标注功能
     *                       "citation_format":  # 角标形式为[ref_i]
     *                       "search_strategy": "pro" # 模型将搜索10条互联网信息
     *                       },
     * @param enableThinking # 是否开启思考模式，适用于 Qwen3 模型。
     *                       Qwen3 商业版模型默认值为 False，Qwen3 开源版模型默认值为 True。
     * @param thinkingBudget 参数设置最大推理过程 Token 数，不能超过供应商要求的最大思维链Token数：例：38912。两个参数对 QwQ 与 DeepSeek-R1 模型无效
     * @return 回复
     */
    public CompletableFuture<Void> generate(Boolean enableSearch,
                                            Map<String, Object> searchOptions,
                                            Boolean enableThinking,
                                            Integer thinkingBudget) {
        if (!isDone()) {
            this.enableSearch = enableSearch;
            this.searchOptions = searchOptions;
            this.enableThinking = enableThinking;
            this.thinkingBudget = thinkingBudget;
            List<ToolSpecification> list = new ArrayList<>(toolMethodList.size());
            for (Tools.ToolMethod wrapper : toolMethodList) {
                list.add(wrapper.toRequest(isSupportChineseToolName));
            }
            List<ChatMessage> messageList = AiUtil.beforeGenerate(chatMemory.messages());
            FunctionCallStreamingResponseHandler fork = fork(this);
            executor.execute(() -> chatModel.generateV2(messageList, list, fork,
                    enableSearch, searchOptions,
                    enableThinking, thinkingBudget, null));
        }
        return this;
    }

    public QuestionClassifyListVO getClassifyListVO() {
        return classifyListVO;
    }

    public Boolean getWebsearch() {
        return websearch;
    }

    public Boolean getReasoning() {
        return reasoning;
    }

    private List<ResultToolExecutor> buildToolExecutor(List<ToolExecutionRequest> toolExecutionRequests, Response<AiMessage> response) {
        List<ResultToolExecutor> executors = new ArrayList<>();
        for (ToolExecutionRequest request : toolExecutionRequests) {
            Tools.ToolMethod wrapper = Tools.ToolMethod.select(toolMethodList, request.name(), isSupportChineseToolName);
            if (wrapper == null) {
                continue;
            }
            SseHttpResponse emitter = newEmitter(toolExecutionRequests, wrapper, response);
            ResultToolExecutor toolExecutor = new ResultToolExecutor(wrapper.tool(), wrapper.method(), wrapper.parameterNames(), wrapper.parameterDefaultValueMap(), request, emitter, memoryId);
            executors.add(toolExecutor);
        }
        return executors;
    }

    public SseHttpResponse toResponse() {
        return new SseHttpResponseImpl(this, null, true);
    }

    protected SseHttpResponse newEmitter(List<ToolExecutionRequest> toolExecutionRequests, Tools.ToolMethod method, Response<AiMessage> response) {
        SseHttpResponseImpl emitter = null;
        if (toolExecutionRequests.size() == 1 && ResultToolExecutor.isEmitterMethod(method.method())) {
            emitter = new SseHttpResponseImpl(this, lastResponse, false);
            aiEmitterList.add(emitter);
        }
        return emitter;
    }

    private CompletableFuture<Tools.ParamValidationResult> executeToolsValidation(
            List<ResultToolExecutor> executors) {
        CompletableFuture<Tools.ParamValidationResult> future = new CompletableFuture<>();
        AtomicInteger done = new AtomicInteger(executors.size());
        for (ResultToolExecutor executor : executors) {
            // 校验
            executor.validation().whenComplete((result, throwable) -> {
                if (throwable != null) {
                    future.completeExceptionally(throwable);
                } else if (result instanceof SseHttpResponse) {
                    future.complete((Tools.ParamValidationResult) result);
                } else if (done.decrementAndGet() == 0) {
                    future.complete(null);
                }
            });
        }
        return future;
    }

    private void exceptionally(Throwable exception) {
        if (isDone()) {
            return;
        }
        this.lastReadTimestamp = READ_DONE;
        bizHandler.onError(exception, baseMessageIndex, addMessageCount.get(), generateCount());
        FunctionCallStreamingResponseHandler handler = this;
        while (handler != null) {
            handler.completeExceptionally(exception);
            handler = handler.parent;
        }
    }

    private int generateCount() {
        return MAX_GENERATE_COUNT - generateRemaining;
    }

    @Override
    public String toString() {
        return "ToolCallStreamingResponseHandler{" + modelName + ":" + toolMethodList.stream().map(Tools.ToolMethod::name).collect(Collectors.joining(",")) + "}";
    }

    private static class SseHttpResponseImpl implements SseHttpResponse {
        private final FunctionCallStreamingResponseHandler handler;
        private final Response<AiMessage> lastResponse;
        private final StringBuilder chatStringBuilder = new StringBuilder();
        private final StringBuilder memoryStringBuilder = new StringBuilder();
        private final LinkedBlockingQueue<Runnable> pendingEventList = new LinkedBlockingQueue<>();
        private final AtomicBoolean closeFlag = new AtomicBoolean(false);
        private volatile boolean toolMessageReady;
        private volatile Response<AiMessage> tokenEnd;
        private volatile boolean empty = true;
        private volatile boolean close = false;

        private SseHttpResponseImpl(FunctionCallStreamingResponseHandler handler,
                                    Response<AiMessage> lastResponse,
                                    boolean toolMessageReady) {
            this.handler = handler;
            this.lastResponse = lastResponse;
            this.toolMessageReady = toolMessageReady;
        }

        private void ready() {
            toolMessageReady = true;
            while (!pendingEventList.isEmpty()) {
                ArrayList<Runnable> events = new ArrayList<>(pendingEventList.size());
                pendingEventList.drainTo(events);
                for (Runnable runnable : events) {
                    runnable.run();
                }
            }
        }

        @Override
        public boolean isEmpty() {
            return empty;
        }

        @Override
        public void write(AiMessageString next) {
            empty = false;
            if (!toolMessageReady) {
                pendingEventList.add(() -> write(next));
                return;
            }
            if (next == null || next.isEmpty()) {
                return;
            }
            String chatString = next.getChatString();
            String memoryString = next.getMemoryString();
            if (chatString != null && !chatString.isEmpty()) {
                chatStringBuilder.append(chatString);
            }
            if (memoryString != null && !memoryString.isEmpty()) {
                memoryStringBuilder.append(memoryString);
            }
            handler.onNext(next);
        }

        @Override
        public boolean isClose() {
            return close;
        }

        @Override
        public void close() {
            close = true;
            empty = false;
            if (!toolMessageReady) {
                pendingEventList.add(this::close);
                return;
            }
            if (!closeFlag.compareAndSet(false, true)) {
                throw new IllegalStateException("close() has already been called");
            }
            if (tokenEnd == null && chatStringBuilder.length() > 0) {
                AiMessage aiMessage = new AiMessage(chatStringBuilder.toString());
                TokenUsage tokenUsage;
                FinishReason finishReason;
                Map<String, Object> metadata = new HashMap<>();
                if (lastResponse != null) {
                    tokenUsage = lastResponse.tokenUsage();
                    finishReason = lastResponse.finishReason();
                    Map<String, Object> lastMetadata = lastResponse.metadata();
                    if (lastMetadata != null) {
                        metadata.putAll(lastMetadata);
                    }
                } else {
                    tokenUsage = new TokenUsage(0, 0, 0);
                    finishReason = FinishReason.STOP;
                }
                String memoryString = memoryStringBuilder.toString();
                metadata.put(MetadataAiMessage.METADATA_KEY_MEMORY_STRING, memoryString);
                tokenEnd = new Response<>(aiMessage, tokenUsage, finishReason, metadata);
                handler.onTokenEnd(tokenEnd);
            }
            handler.done(tokenEnd);
        }

        @Override
        public void close(Throwable error) {
            Objects.requireNonNull(error, "error cannot be null");
            close = true;
            empty = false;
            if (!toolMessageReady) {
                pendingEventList.add(() -> close(error));
                return;
            }
            if (!closeFlag.compareAndSet(false, true)) {
                throw new IllegalStateException("close() has already been called");
            }
            handler.onError(error);
        }
    }

}
