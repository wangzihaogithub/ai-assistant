package com.github.aiassistant.service.text;

import com.github.aiassistant.entity.model.chat.MemoryIdVO;
import com.github.aiassistant.entity.model.chat.QuestionClassifyListVO;
import com.github.aiassistant.entity.model.langchain4j.LangChainUserMessage;
import com.github.aiassistant.entity.model.langchain4j.MetadataAiMessage;
import com.github.aiassistant.exception.AiAssistantException;
import com.github.aiassistant.exception.ModelApiGenerateException;
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
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.model.StreamingResponseHandler;
import dev.langchain4j.model.openai.AudioChunk;
import dev.langchain4j.model.openai.AudioStreamingResponseHandler;
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
public class FunctionCallStreamingResponseHandler extends CompletableFuture<Void> implements ThinkingStreamingResponseHandler, AudioStreamingResponseHandler, StreamingResponseHandler<AiMessage> {
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
    private final AtomicBoolean generate = new AtomicBoolean(false);
    /**
     * 供应商支持的接口参数
     */
    private final GenerateRequest request;
    private Tools.ParamValidationResult validationResult;
    private Response<AiMessage> lastResponse;
    private volatile long lastReadTimestamp;
    private volatile ScheduledFuture<?> readTimeoutFuture;
    /**
     * 自动确认的内容
     */
    private String confirmToolCall = "ok";
    /**
     * 向供应商发起请求前的回调钩子，可用于请求前填充参数
     */
    private BiConsumer<FunctionCallStreamingResponseHandler, GenerateRequest> beforeRequestConsumer;

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
        for (Tools.ToolMethod toolMethod : toolMethodList) {
            toolMethod.tool().setStreamingResponseHandler(handler);
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
        this.request = new GenerateRequest();
        this.reasoning = reasoning;
        this.addMessageCount = new AtomicInteger(addMessageCount);
        this.generateRemaining = MAX_GENERATE_COUNT;
    }

    protected FunctionCallStreamingResponseHandler(FunctionCallStreamingResponseHandler parent) {
        this.readTimeoutMs = parent.readTimeoutMs;
        this.modelName = parent.modelName;
        this.memoryId = parent.memoryId;
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
        this.beforeRequestConsumer = parent.beforeRequestConsumer;
        this.request = parent.request.clone();
    }

    /**
     * 创建下一次请求的回掉逻辑，递归（如果需要继续生成）拉一条链表
     *
     * @param parent 上一个回掉逻辑
     * @return 下一个回掉逻辑
     */
    protected FunctionCallStreamingResponseHandler copy(FunctionCallStreamingResponseHandler parent) {
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
        // 如果已完成
        if (isDone()) {
            // 完成后重复触发了异常，这种情况理论上不存在，除非吐字超时 和 吐字异常 和 吐字完成 任其二同时刻触发了
            log.error("done after onError={}", error.toString(), error);
            return;
        }
        // 解构异常
        Throwable rootError = error;
        while (rootError instanceof CompletionException) {
            rootError = rootError.getCause();
        }
        AiAssistantException assistantException;
        if (rootError instanceof AiAssistantException) {
            assistantException = (AiAssistantException) rootError;
        } else {
            Optional<List<ChatMessage>> messageList = Optional.ofNullable(parent).map(e -> e.request).map(GenerateRequest::getMessageList);
            String lastUserQuestion = messageList.map(AiUtil::getLastUserQuestion).orElse(null);
            assistantException = new ModelApiGenerateException(
                    String.format("llm generate error! lastUserQuestion = '%s', %s , %s", lastUserQuestion, name(), rootError),
                    error, modelName, memoryId, messageList.orElse(null));
        }
        // 读标记为完成
        this.lastReadTimestamp = READ_DONE;
        // 事件Hook通知 onError
        bizHandler.onError(assistantException, baseMessageIndex, addMessageCount.get(), generateCount());

        // 触发异步完成
        FunctionCallStreamingResponseHandler handler = this;
        while (handler != null) {
            handler.completeExceptionally(assistantException);
            handler = handler.parent;
        }
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
            // 解决模型生成工具调用不稳定
            if (AiUtil.isErrorAiToolMessage(response)) {
                // 删掉模型生成的错误调用
                response = AiUtil.filterErrorToolRequestId(response);
            }
            // function call if need
            functionCallIfNeed(response);
        } catch (Exception e) {
            onError(e);
        }
    }

    /**
     * 语音回复（多模态）
     * wav_bytes = base64.b64decode(audio_string)
     * audio_np = np.frombuffer(wav_bytes, dtype=np.int16)
     * sf.write("audio_assistant_py.wav", audio_np, samplerate=24000)
     * <p>
     * 输出音频的音色与文件格式（只支持设定为"wav"）通过audio参数来配置，
     * 如：audio={"voice": "Cherry", "format": "wav"}，
     * 其中商业版模型voice参数可选值为：["Cherry", "Serena", "Ethan", "Chelsie"]，
     * 开源版模型voice参数可选值为：["Ethan", "Chelsie"]。
     * https://bailian.console.aliyun.com/?tab=api#/api/?type=model&url=https%3A%2F%2Fhelp.aliyun.com%2Fdocument_detail%2F2867839.html
     *
     * @param audioChunk 部分语音回复块
     */
    @Override
    public void onAudio(AudioChunk audioChunk) {
        try {
            bizHandler.onAudio(audioChunk);
        } catch (Exception e) {
            log.error("onAudio failed {}", e.toString(), e);
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
        // 如果需要超时倒计时
        if (readTimeoutFuture == null && readTimeoutMs != null) {
            synchronized (this) {
                if (readTimeoutFuture == null) {
                    // 倒计时
                    this.readTimeoutFuture = READ_TIMEOUT_SCHEDULED.scheduleWithFixedDelay(this::timeoutCheck, readTimeoutMs, Math.max(readTimeoutMs / 2, 500), TimeUnit.MILLISECONDS);
                }
            }
        }
        // 记录最后一次吐字时间
        this.lastReadTimestamp = System.currentTimeMillis();

        // 存在工具入参校验结果
        Tools.ParamValidationResult validationResult = this.validationResult;
        if (validationResult != null) {
            this.validationResult = null;
            // 给前端吐工具入参校验结果
            appendAiMessageValidationResult(validationResult.getAiMessage());
        }
        try {
            // 给前端吐字
            bizHandler.onToken(token, baseMessageIndex, addMessageCount.get());
        } catch (Exception e) {
            onError(e);
        }
    }

    private void timeoutCheck() {
        long lastReadTimestamp = this.lastReadTimestamp;
        // 如果没有超时，因为再规定事件内完成了
        if (lastReadTimestamp == READ_DONE) {
            readTimeoutFuture.cancel(false);
        } else if ((System.currentTimeMillis() - lastReadTimestamp) >= readTimeoutMs) {
            // 超时了
            readTimeoutFuture.cancel(false);
            long timeout = System.currentTimeMillis() - lastReadTimestamp;
            // 建一个超时异常
            TokenReadTimeoutException exception = new TokenReadTimeoutException(String.format("llm generate error! tokenReadTimeout modelName '%s' readTimeout %s/ms, config '%sms'", modelName, timeout, readTimeoutMs), timeout, readTimeoutMs);
            // 推错误事件
            onError(exception);
        }
    }

    private void appendAiMessageValidationResult(AiMessage validationMessage) {
        // 吐字
        onNext(new AiMessageString(validationMessage.text()));
        // 事件Hook通知 onTokenEnd
        hookOnTokenEnd(new Response<>(validationMessage, lastResponse.tokenUsage(), lastResponse.finishReason(), lastResponse.metadata()));
        // 事件Hook通知 onTokenBegin
        hookOnTokenBegin();
    }

    private void functionCallIfNeed(Response<AiMessage> response) {
        AiMessage aiMessage = response.content();
        boolean typeThinkingAiMessage = AiUtil.isTypeThinkingAiMessage(aiMessage);
        // 如果AI返回了工具调用清单
        if (aiMessage.hasToolExecutionRequests()) {
            // 构造出：多个工具执行器
            List<ResultToolExecutor> toolExecutors = buildToolExecutor(aiMessage.toolExecutionRequests(), response);
            // 校验模型给过来的工具入参，再进行工具调用
            executeToolsValidationAndCall(toolExecutors, response).whenComplete((validationFail, throwable) -> {
                if (throwable != null) {
                    // 工具调用报错
                    onError(throwable);
                } else if (validationFail != null) {
                    // 工具入参验证不通过，不能调用工具，给用户返回不能调用的原因。
                    hookOnComplete(validationFail);
                } else {
                    // 是否执行工具的中途，被工具直接给前端返回结果了
                    boolean isEmitter = toolExecutors.stream().allMatch(ResultToolExecutor::isEmitter);
                    // 取出每个工具执行后，返回的结果
                    List<ToolExecutionResultMessage> resultMessageList = toolExecutors.stream()
                            .map(e -> e.getNow(null))
                            .collect(Collectors.toList());
                    // 添加到记忆中(chatMemory)
                    for (ToolExecutionResultMessage tm : resultMessageList) {
                        addMessage(tm);
                    }
                    // 所有工具已调用完毕，将工具执行途中给前端返回的结果进行推送前端
                    for (SseHttpResponseImpl emitter : aiEmitterList) {
                        emitter.ready();
                    }
                    // 如果没有工具给前端推送消息
                    if (!isEmitter || aiEmitterList.stream().allMatch(SseHttpResponseImpl::isEmpty)) {
                        // 再次向AI生成一次。注：会附带上刚才添加到记忆中(chatMemory)的消息
                        generate();
                    }
                }
            });
        } else {
            // 是否需要自动帮用户进行信息确认
            isNeedConfirmToolCall(response).whenComplete((b, throwable) -> {
                // 需要帮用户进行信息确认
                if (Boolean.TRUE.equals(b)) {
                    // 事件Hook通知 onTokenEnd
                    hookOnTokenEnd(response);
                    // 事件Hook通知 onTokenBegin
                    hookOnTokenBegin();
                    // 添加一条自动帮用户确认的消息
                    addMessage(new LangChainUserMessage(confirmToolCall));
                    // 再次向AI生成一次。注：会附带上自动帮用户确认的消息
                    generate();
                } else {
                    // 不帮用户进行信息确认
                    // 事件Hook通知 onTokenEnd
                    hookOnTokenEnd(response);
                    // 事件Hook通知 onComplete
                    hookOnComplete(response);
                }
            });
        }
    }

    /**
     * 校验模型给过来的工具入参，再进行工具调用
     *
     * @param toolExecutors 使用工具
     * @param response      AI返回的工具调用清单
     * @return 工具调用结果
     */
    private CompletableFuture<Response<AiMessage>> executeToolsValidationAndCall(List<ResultToolExecutor> toolExecutors, Response<AiMessage> response) {
        CompletableFuture<Response<AiMessage>> future = new CompletableFuture<>();
        // 在开始工具执行前，先校验模型给过来的工具入参
        CompletableFuture<Tools.ParamValidationResult> validationResultFuture = executeToolsValidation(toolExecutors);
        // 校验工具入参后，再进行工具调用
        validationResultFuture.whenComplete((validationResult, throwable) -> {
            this.validationResult = validationResult;
            try {
                if (throwable != null) {
                    // 校验工具入参的代码报错了
                    future.completeExceptionally(throwable);
                } else if (validationResult != null && validationResult.isStopExecute()) {
                    // 参数验证不通过，不能调用工具，给用户返回不能调用的原因。
                    Response<AiMessage> validationResponse = new Response<>(validationResult.getAiMessage(),
                            response.tokenUsage(), response.finishReason(), response.metadata());
                    // 事件Hook通知 onTokenBegin
                    hookOnTokenEnd(validationResponse);
                    future.complete(validationResponse);
                } else {
                    // 参数验证通过，开始调用工具
                    hookOnTokenEnd(response);
                    // 事件Hook通知 onTokenBegin
                    hookOnTokenBegin();
                    // 事件Hook通知 onToolCalls
                    bizHandler.onToolCalls(response);
                    // 执行工具调用
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
            executor.execute().whenComplete(new BiConsumer<ToolExecutionResultMessage, Throwable>() {
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

    /**
     * 是否需要自动帮用户进行信息确认
     * 例：AI回复：接下来我将自动帮您进行查询。 自动回复：好
     *
     * @param response AI返回的内容
     * @return true=需要自动帮用户进行信息确认
     * @see AiUtil#isNeedConfirmToolCall(Response, Collection) 可以看下这个案例
     */
    private CompletableFuture<Boolean> isNeedConfirmToolCall(Response<AiMessage> response) {
        if (generateRemaining <= 0) {
            return CompletableFuture.completedFuture(false);
        }
//        if (tools.isEmpty()) {
//            return CompletableFuture.completedFuture(false);
//        }
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

    private void hookOnComplete(Response<AiMessage> response) {
        bizHandler.onComplete(response, baseMessageIndex, addMessageCount.get(), generateCount());
        FunctionCallStreamingResponseHandler handler = this;
        while (handler != null) {
            handler.complete(null);
            handler = handler.parent;
        }
    }

    private void hookOnTokenEnd(Response<AiMessage> response) {
        addMessage(MetadataAiMessage.convert(response));
        bizHandler.onTokenEnd(response, baseMessageIndex, addMessageCount.get(), generateCount());
    }

    private void hookOnTokenBegin() {
        bizHandler.onTokenBegin(baseMessageIndex, addMessageCount.get(), generateCount());
    }

    public void addMessage(ChatMessage message) {
        chatMemory.add(message);
        addMessageCount.incrementAndGet();
    }

    /**
     * 生成AI回复
     *
     * @return 最终完成后，会异步触发
     */
    public CompletableFuture<Void> generate() {
        return generate(beforeRequestConsumer);
    }

    /**
     * 生成AI回复
     *
     * @param beforeRequestConsumer 向供应商发起请求前的回调钩子，可用于请求前填充参数
     * @return 最终完成后，会异步触发
     */
    public CompletableFuture<Void> generate(BiConsumer<FunctionCallStreamingResponseHandler, GenerateRequest> beforeRequestConsumer) {
        // 如果没完成，且没请求过
        if (!isDone() && generate.compareAndSet(false, true)) {
            try {
                // 过滤重复消息
                request.messageList = new ArrayList<>(AiUtil.beforeGenerate(chatMemory.messages()));
                // 工具
                request.toolSpecificationList = toolMethodList.stream().map(e -> e.toRequest(isSupportChineseToolName)).collect(Collectors.toList());
                // 用户可以自定义填充参数
                if (beforeRequestConsumer != null) {
                    beforeRequestConsumer.accept(this, request);
                }
                this.beforeRequestConsumer = beforeRequestConsumer;
                executor.execute(() -> {
                    try {
                        // copy创建下一次请求的回掉逻辑，递归（如果需要继续生成）拉一条链表
                        // request请求聊天大模型
                        chatModel.request(copy(this), request);
                    } catch (Exception e) {
                        onError(e);
                    }
                });
            } catch (Exception e) {
                onError(e);
            }
        }
        return this;
    }

    /**
     * 获取模型生成次数
     *
     * @return 模型生成次数
     */
    public int getGenerateCount() {
        int count = 0;
        FunctionCallStreamingResponseHandler handler = this;
        while (handler != null && handler.generate.get()) {
            handler = handler.parent;
            count++;
        }
        return count;
    }

    /**
     * 构造出：多个工具执行器
     *
     * @param toolExecutionRequests 工具请求
     * @param response              AI返回的工具调用清单
     * @return 工具执行器
     */
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

    /**
     * 转成可以给前端推送消息的SseHttpResponse
     *
     * @return SseHttpResponse 可以主动给前端推送消息
     */
    public SseHttpResponse toResponse() {
        return new SseHttpResponseImpl(this, null, true);
    }

    /**
     * 创建可以给前端推送消息的SseHttpResponse
     *
     * @param toolExecutionRequests 工具请求
     * @param method                本地方法
     * @param response              AI返回的工具调用清单
     * @return SseHttpResponse 可以主动给前端推送消息
     */
    protected SseHttpResponse newEmitter(List<ToolExecutionRequest> toolExecutionRequests, Tools.ToolMethod method, Response<AiMessage> response) {
        SseHttpResponseImpl emitter = null;
        if (toolExecutionRequests.size() == 1 && ResultToolExecutor.isEmitterMethod(method.method())) {
            emitter = new SseHttpResponseImpl(this, lastResponse, false);
            aiEmitterList.add(emitter);
        }
        return emitter;
    }

    /**
     * 在开始工具执行前，先校验模型给过来的工具入参
     *
     * @param executors 使用工具
     * @return 如果参数验证不通过，不能调用工具，会给用户返回不能调用的原因。
     */
    private CompletableFuture<Tools.ParamValidationResult> executeToolsValidation(
            List<ResultToolExecutor> executors) {
        CompletableFuture<Tools.ParamValidationResult> future = new CompletableFuture<>();
        AtomicInteger done = new AtomicInteger(executors.size());
        for (ResultToolExecutor executor : executors) {
            // 执行校验
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

    private int generateCount() {
        return MAX_GENERATE_COUNT - generateRemaining;
    }

    public String getConfirmToolCall() {
        return confirmToolCall;
    }

    public void setConfirmToolCall(String confirmToolCall) {
        this.confirmToolCall = confirmToolCall;
    }

    public GenerateRequest getRequest() {
        return request;
    }

    public String getModelName() {
        return modelName;
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

    @Override
    public String toString() {
        return "FunctionCallStreamingResponseHandler{" + modelName + ":" + toolMethodList.stream().map(Tools.ToolMethod::name).collect(Collectors.joining(",")) + "}";
    }

    public String name() {
        return "FunctionCallStreamingResponseHandler{" + modelName + "}";
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

        /**
         * 所有工具已调用完毕，将工具执行途中给前端返回的结果进行推送前端
         */
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

        /**
         * 是否没有推送过内容（即：没调用过 write）
         *
         * @return true=没有推送过内容
         */
        @Override
        public boolean isEmpty() {
            return empty;
        }

        /**
         * 给前端推送内容
         *
         * @param messageString 推送内容
         */
        @Override
        public void write(AiMessageString messageString) {
            empty = false;
            if (!toolMessageReady) {
                pendingEventList.add(() -> write(messageString));
                return;
            }
            if (messageString == null || messageString.isEmpty()) {
                return;
            }
            String chatString = messageString.getChatString();
            String memoryString = messageString.getMemoryString();
            if (chatString != null && !chatString.isEmpty()) {
                chatStringBuilder.append(chatString);
            }
            if (memoryString != null && !memoryString.isEmpty()) {
                memoryStringBuilder.append(memoryString);
            }
            handler.onNext(messageString);
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
                handler.hookOnTokenEnd(tokenEnd);
            }
            handler.hookOnComplete(tokenEnd);
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
