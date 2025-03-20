package com.github.aiassistant.service.text;

import com.github.aiassistant.entity.model.chat.IDAiMessage;
import com.github.aiassistant.entity.model.chat.LangChainUserMessage;
import com.github.aiassistant.entity.model.chat.MemoryIdVO;
import com.github.aiassistant.service.jsonschema.LlmJsonSchemaApiService;
import com.github.aiassistant.service.jsonschema.WhetherWaitingForAiJsonSchema;
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
import dev.langchain4j.model.StreamingResponseHandler;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.output.FinishReason;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.output.TokenUsage;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/**
 * 流式处理AI结果（同时调用工具库）
 */
public class ToolCallStreamingResponseHandler extends CompletableFuture<Void> implements StreamingResponseHandler<AiMessage> {
    public static final int MAX_GENERATE_COUNT = 10;
    public static final long READ_DONE = -1L;
    private static final ScheduledThreadPoolExecutor READ_TIMEOUT_SCHEDULED = new ScheduledThreadPoolExecutor(1, new ThreadFactory() {
        private final AtomicInteger threadCount = new AtomicInteger(0);

        @Override
        public Thread newThread(@NotNull Runnable r) {
            Thread thread = new Thread(r, "ai-readTimeout" + threadCount.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        }
    });
    private final Object memoryId;
    private final ChatMemory chatMemory;
    private final String modelName;
    private final StreamingChatLanguageModel chatModel;
    private final List<Tools> tools;
    private final List<Tools.ToolMethod> toolMethodList;
    private final ToolCallStreamingResponseHandler parent;
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
    private Tools.ParamValidationResult validationResult;
    private Response<AiMessage> lastResponse;
    private volatile long lastReadTimestamp;
    private volatile ScheduledFuture<?> readTimeoutFuture;

    public ToolCallStreamingResponseHandler(String modelName, StreamingChatLanguageModel chatModel,
                                            ChatMemory chatMemory,
                                            ChatStreamingResponseHandler handler,
                                            LlmJsonSchemaApiService llmJsonSchemaApiService,
                                            List<Tools.ToolMethod> toolMethodList,
                                            boolean isSupportChineseToolName,
                                            int baseMessageIndex,
                                            int addMessageCount,
                                            Long readTimeoutMs,
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
        this.addMessageCount = new AtomicInteger(addMessageCount);
        this.generateRemaining = MAX_GENERATE_COUNT;
    }

    protected ToolCallStreamingResponseHandler(ToolCallStreamingResponseHandler parent) {
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
        this.generateRemaining = parent.generateRemaining - 1;
        this.validationResult = parent.validationResult;
        this.lastResponse = parent.lastResponse;
    }

    protected ToolCallStreamingResponseHandler fork(ToolCallStreamingResponseHandler parent) {
        return new ToolCallStreamingResponseHandler(parent);
    }

    @Override
    public String toString() {
        return "ToolCallStreamingResponseHandler{" + modelName + ":" + toolMethodList.stream().map(Tools.ToolMethod::name).collect(Collectors.joining(",")) + "}";
    }

    private void timeoutCheck() {
        long lastReadTimestamp = this.lastReadTimestamp;
        if (lastReadTimestamp == READ_DONE) {
            readTimeoutFuture.cancel(false);
        } else if ((System.currentTimeMillis() - lastReadTimestamp) >= readTimeoutMs) {
            readTimeoutFuture.cancel(false);
            onTokenReadTimeout(System.currentTimeMillis() - lastReadTimestamp, readTimeoutMs);
        }
    }

    @Override
    public void onNext(String token) {
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

    private void appendAiMessage(AiMessage message) {
        Response<AiMessage> validationResponse = new Response<>(message, lastResponse.tokenUsage(), lastResponse.finishReason(), lastResponse.metadata());
        this.validationResult = null;
        onNext(message.text());
        onTokenEnd(validationResponse);
        bizHandler.onTokenBegin(baseMessageIndex, addMessageCount.get(), generateCount());
    }

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

    private void onComplete0(Response<AiMessage> response) {
        AiMessage aiMessage = response.content();
        if (aiMessage.hasToolExecutionRequests()) {
            List<ResultToolExecutor> toolExecutors = buildToolExecutor(aiMessage.toolExecutionRequests(), response);
            executeToolsValidationAndCall(toolExecutors, response).whenComplete((toolResponse, throwable) -> {
                if (throwable != null) {
                    onError(throwable);
                } else if (toolResponse != null) {
                    done(toolResponse);
                } else {
                    boolean isEmitter = toolExecutors.stream().allMatch(ResultToolExecutor::isEmitter);
                    List<ToolExecutionResultMessage> resultMessageList = toolExecutors.stream()
                            .map(e -> e.getNow(null))
                            .collect(Collectors.toList());
                    for (ToolExecutionResultMessage tm : resultMessageList) {
                        addMessage(tm);
                    }
                    for (SseHttpResponseImpl emitter : aiEmitterList) {
                        emitter.toolMessageReady();
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
        if (AiUtil.isNeedConfirmToolCall(response, toolMethodList)) {
            return CompletableFuture.completedFuture(true);
        }
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
        ToolCallStreamingResponseHandler handler = this;
        while (handler != null) {
            handler.complete(null);
            handler = handler.parent;
        }
    }

    private void onTokenEnd(Response<AiMessage> response) {
        addMessage(IDAiMessage.convert(response));
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
        if (!isDone()) {
            List<ToolSpecification> list = new ArrayList<>(toolMethodList.size());
            for (Tools.ToolMethod wrapper : toolMethodList) {
                list.add(wrapper.toRequest(isSupportChineseToolName));
            }
            List<ChatMessage> messageList = AiUtil.beforeGenerate(chatMemory.messages());
            ToolCallStreamingResponseHandler fork = fork(this);
            executor.execute(() -> chatModel.generate(messageList, list, fork));
        }
        return this;
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
        return new SseHttpResponseImpl(this);
    }

    protected SseHttpResponse newEmitter(List<ToolExecutionRequest> toolExecutionRequests, Tools.ToolMethod method, Response<AiMessage> response) {
        SseHttpResponseImpl emitter = null;
        if (toolExecutionRequests.size() == 1 && ResultToolExecutor.isEmitterMethod(method.method())) {
            emitter = new SseHttpResponseImpl(this, response);
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

    private void onTokenReadTimeout(long timeout, long readTimeoutMs) {
        TimeoutException exception = new TimeoutException(String.format("llm error! onTokenReadTimeout modelName '%s' readTimeout %s/ms, config '%sms'", modelName, timeout, readTimeoutMs));
        exceptionally(exception);
    }

    private void exceptionally(Throwable exception) {
        if (isDone()) {
            return;
        }
        this.lastReadTimestamp = READ_DONE;
        bizHandler.onError(exception, baseMessageIndex, addMessageCount.get(), generateCount());
        ToolCallStreamingResponseHandler handler = this;
        while (handler != null) {
            handler.completeExceptionally(exception);
            handler = handler.parent;
        }
    }

    @Override
    public void onError(Throwable throwable) {
        Throwable rootError = throwable instanceof CompletionException ? throwable.getCause() : throwable;
        ExecutionException exception = new ExecutionException(
                "llm error! modelName=" + modelName + " " + rootError.getMessage(),
                rootError);
        exceptionally(exception);
    }

    private int generateCount() {
        return MAX_GENERATE_COUNT - generateRemaining;
    }

    public static class SseHttpResponseImpl implements SseHttpResponse {
        private final ToolCallStreamingResponseHandler handler;
        private final Response<AiMessage> lastResponse;
        private final StringBuilder builder = new StringBuilder();
        private final AtomicBoolean toolMessageReady = new AtomicBoolean();
        private final LinkedBlockingQueue<Runnable> pendingEventList = new LinkedBlockingQueue<>();
        private volatile Response<AiMessage> tokenEnd;
        private volatile boolean empty = true;

        public SseHttpResponseImpl(ToolCallStreamingResponseHandler handler, Response<AiMessage> lastResponse) {
            this.handler = handler;
            this.lastResponse = lastResponse;
        }

        public SseHttpResponseImpl(ToolCallStreamingResponseHandler handler) {
            this.handler = handler;
            this.lastResponse = new Response<>(AiUtil.NULL, new TokenUsage(0, 0, 0), FinishReason.STOP);
            toolMessageReady.set(true);
        }

        void toolMessageReady() {
            if (toolMessageReady.compareAndSet(false, true)) {
                while (!pendingEventList.isEmpty()) {
                    ArrayList<Runnable> events = new ArrayList<>(pendingEventList.size());
                    pendingEventList.drainTo(events);
                    for (Runnable runnable : events) {
                        runnable.run();
                    }
                }
            }
        }

        @Override
        public boolean isEmpty() {
            return empty;
        }

        @Override
        public void write(String next) {
            empty = false;
            if (!toolMessageReady.get()) {
                pendingEventList.add(() -> write(next));
                return;
            }
            if (next == null || next.isEmpty()) {
                return;
            }
            builder.append(next);
            handler.onNext(next);
        }

        @Override
        public void close() {
            empty = false;
            if (!toolMessageReady.get()) {
                pendingEventList.add(this::close);
                return;
            }
            if (tokenEnd == null) {
                AiMessage aiMessage = new AiMessage(builder.toString());
                tokenEnd = lastResponse != null ? new Response<>(aiMessage,
                        lastResponse.tokenUsage(),
                        lastResponse.finishReason(),
                        lastResponse.metadata()) : new Response<>(aiMessage);
                handler.onTokenEnd(tokenEnd);
            }
            handler.done(tokenEnd);
        }

        @Override
        public void close(Throwable error) {
            empty = false;
            if (!toolMessageReady.get()) {
                pendingEventList.add(() -> close(error));
                return;
            }
            handler.onError(error);
        }
    }

}
