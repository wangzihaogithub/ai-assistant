package com.github.aiassistant.service.text.sseemitter;


import com.github.aiassistant.entity.model.chat.AiVariablesVO;
import com.github.aiassistant.entity.model.chat.QaKnVO;
import com.github.aiassistant.entity.model.chat.QuestionClassifyListVO;
import com.github.aiassistant.entity.model.chat.WebSearchResultVO;
import com.github.aiassistant.enums.AiErrorTypeEnum;
import com.github.aiassistant.enums.AiWebSearchSourceEnum;
import com.github.aiassistant.enums.UserTriggerEventEnum;
import com.github.aiassistant.service.jsonschema.ReasoningJsonSchema;
import com.github.aiassistant.service.text.ChatStreamingResponseHandler;
import com.github.aiassistant.service.text.acting.ActingService;
import com.github.aiassistant.service.text.tools.functioncall.UrlReadTools;
import com.github.aiassistant.util.AiUtil;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.openai.AudioChunk;
import dev.langchain4j.model.output.FinishReason;
import dev.langchain4j.model.output.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * SseEmitter流式推送前端
 */
public class SseEmitterResponseHandler implements ChatStreamingResponseHandler {
    private static final Logger log = LoggerFactory.getLogger(SseEmitterResponseHandler.class);
    private final Emitter emitter;
    private final boolean debug;
    private final List<OnWebSearchRead> onWebSearchReadList = new ArrayList<>();
    private final Boolean websearch;
    private final String userQueryTraceNumber;
    private int baseMessageIndex;
    private int addMessageCount;
    private boolean sendComplete = false;
    private boolean close;
    private int tokenBeginIndex = 0;
    private volatile CompleteEvent completeEvent;
    private int generateCount;
    private boolean acceptReasoningEvent = false;
    private int eventId;

    public SseEmitterResponseHandler(Emitter emitter,
                                     boolean debug,
                                     String userQueryTraceNumber,
                                     Boolean websearch) {
        this.emitter = emitter;
        this.debug = debug;
        this.websearch = websearch;
        this.userQueryTraceNumber = userQueryTraceNumber;
        sendToClient(emitter, "session-create",
                "userQueryTraceNumber", userQueryTraceNumber);
    }

    public static void sendError(Emitter emitter, String errorType, String error) throws IOException {
        Map<String, Object> errorMap = new HashMap<>();
        errorMap.put("errorType", errorType);
        errorMap.put("error", error);
        errorMap.put("finishReason", "API_ERROR");
        emitter.send(null, "api-error", errorMap);
        emitter.send(null, "complete", errorMap);
    }

    public Emitter getEmitter() {
        return emitter;
    }

    public boolean isSendComplete() {
        return sendComplete;
    }

    public boolean isAcceptReasoningEvent() {
        return acceptReasoningEvent;
    }

    public boolean isClose() {
        return close;
    }

    public String getUserQueryTraceNumber() {
        return userQueryTraceNumber;
    }

    public Boolean getWebsearch() {
        return websearch;
    }

    public boolean isDebug() {
        return debug;
    }

    public int generateCount() {
        return generateCount;
    }

    public int baseMessageIndex() {
        return baseMessageIndex;
    }

    public int addMessageCount() {
        return addMessageCount;
    }

    @Override
    public void onToken(AiMessageString token, int baseMessageIndex, int addMessageCount) {
        this.baseMessageIndex = baseMessageIndex;
        this.addMessageCount = addMessageCount;
        sendToClient(emitter, "token-chunk",
                "baseMessageIndex", baseMessageIndex,
                "addMessageCount", addMessageCount,
                "messageIndex", baseMessageIndex + addMessageCount,
                "token", token.getChatString());
    }

    @Override
    public void onAudio(AudioChunk audioChunk) {
        sendToClient(emitter, "audio-chunk",
                "audioBase64", audioChunk.getBase64(),
                "transcript", audioChunk.getTranscript());
    }

    @Override
    public void onModelThinkingToken(String thinkingToken) {
        sendToClient(emitter, "token-chunk-model-thinking",
                "baseMessageIndex", baseMessageIndex,
                "addMessageCount", addMessageCount,
                "messageIndex", baseMessageIndex + addMessageCount,
                "token", thinkingToken);
    }

    @Override
    public void onBeforeModelThinking() {
        sendToClient(emitter, "token-begin-model-thinking",
                "baseMessageIndex", baseMessageIndex,
                "addMessageCount", addMessageCount,
                "messageIndex", baseMessageIndex + addMessageCount);
    }

    @Override
    public void onAfterModelThinking(Response<AiMessage> thinkingResponse) {
        sendToClient(emitter, "token-end-model-thinking",
                "baseMessageIndex", baseMessageIndex,
                "addMessageCount", addMessageCount,
                "messageIndex", baseMessageIndex + addMessageCount,
                "thinkingContent", thinkingResponse.content().text());
    }

    @Override
    public void onError(Throwable error, int baseMessageIndex, int addMessageCount, int generateCount) {
        this.baseMessageIndex = baseMessageIndex;
        this.addMessageCount = addMessageCount;
        this.generateCount = generateCount;

        AiErrorTypeEnum errorTypeEnum = AiErrorTypeEnum.parseErrorType(error);
        sendToClient(emitter, "api-error",
                "errorType", errorTypeEnum.getCode(),
                "messageText", errorTypeEnum.getMessageText(),
                "error", debug ? error.toString() : "hidden");
        log.error("sse api error {}", error.toString(), error);

        try {
            complete(emitter, null, error, "API_ERROR", baseMessageIndex, addMessageCount);
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.debug("Error completing emitter {}", e.toString(), e);
            }
        } finally {
            this.close = true;
        }
    }

    @Override
    public void onTokenBegin(int baseMessageIndex, int addMessageCount, int generateCount) {
        this.baseMessageIndex = baseMessageIndex;
        this.addMessageCount = addMessageCount;
        this.generateCount = generateCount;
        sendToClient(emitter, "token-begin",
                "baseMessageIndex", baseMessageIndex,
                "addMessageCount", addMessageCount,
                "messageIndex", baseMessageIndex + addMessageCount,
                "count", ++tokenBeginIndex);
    }

    @Override
    public void onTokenEnd(Response<AiMessage> response, int baseMessageIndex, int addMessageCount, int generateCount) {
        this.baseMessageIndex = baseMessageIndex;
        this.addMessageCount = addMessageCount;
        this.generateCount = generateCount;
        AiMessage aiMessage = response.content();
        boolean typeThinkingAiMessage = AiUtil.isTypeThinkingAiMessage(aiMessage);
        String text = aiMessage.text();
        FinishReason finishReason = response.finishReason();
        sendToClient(emitter, "token-end",
                "baseMessageIndex", baseMessageIndex,
                "addMessageCount", addMessageCount,
                "messageIndex", baseMessageIndex + addMessageCount,
                "finishReason", finishReason != null ? finishReason.name() : null,
                "thinking", typeThinkingAiMessage,
                "text", typeThinkingAiMessage ? "" : text);
    }

    @Override
    public void onJsonSchemaToolCalls(Response<AiMessage> response) {
        if (debug) {
            AiMessage aiMessage = response.content();
            List<String> nameList = aiMessage.toolExecutionRequests().stream().map(ToolExecutionRequest::name).collect(Collectors.toList());
            List<Map<String, Object>> requestList = aiMessage.toolExecutionRequests().stream().map(e -> {
                Map<String, Object> request = new HashMap<>();
                request.put("name", e.name());
                request.put("arguments", e.arguments());
                return request;
            }).collect(Collectors.toList());
            sendToClient(emitter, "json-schema-tool-calls",
                    "finishReason", response.finishReason().name(),
                    "nameList", nameList,
                    "requestList", requestList);
        }
    }

    @Override
    public void afterWebSearch(AiWebSearchSourceEnum sourceEnum, String providerName, String question, WebSearchResultVO resultVO, long cost) {
        onWebSearchReadList.add(new OnWebSearchRead(sourceEnum, providerName, question, resultVO, cost));
        sendOnWebSearchRead(sourceEnum, providerName, question, resultVO, cost, false);
    }

    @Override
    public void onBeforeReasoningAndActing(boolean reasoningAndActing) {
        sendToClient(emitter, "before-reasoning-and-acting",
                "reasoningAndActing", reasoningAndActing);
    }

    @Override
    public void beforeWebSearch(AiWebSearchSourceEnum sourceEnum, String providerName, String question) {
        sendToClient(emitter, "before-websearch",
                "question", question,
                "providerName", providerName,
                "sourceEnum", sourceEnum);
    }

    @Override
    public void afterUrlRead(AiWebSearchSourceEnum sourceEnum, String providerName, String question, UrlReadTools urlReadTools, WebSearchResultVO.Row row, String content, String merge, long cost) {
        sendToClient(emitter, "after-urlread",
                "question", question,
                "providerName", providerName,
                "sourceEnum", sourceEnum,
                "row", row,
                "cost", cost,
                "proxy", urlReadTools.getProxyVO(),
                "content", content,
                "merge", merge);
    }

    @Override
    public void beforeUrlRead(AiWebSearchSourceEnum sourceEnum, String providerName, String question, UrlReadTools urlReadTools, WebSearchResultVO.Row row) {
        sendToClient(emitter, "before-urlread",
                "question", question,
                "providerName", providerName,
                "sourceEnum", sourceEnum,
                "row", row);
    }

    private void sendOnWebSearchRead(AiWebSearchSourceEnum sourceEnum, String providerName, String question,
                                     WebSearchResultVO resultVO, long cost, boolean replay) {
        sendToClient(emitter, "after-websearch",
                "replay", replay,
                "count", Optional.ofNullable(resultVO).map(WebSearchResultVO::getList).map(List::size).orElse(0),
                "question", question,
                "providerName", providerName,
                "cost", cost,
                "sourceEnum", sourceEnum == null ? null : sourceEnum.getCode(),
                "webSearchResult", resultVO);
    }

    @Override
    public void onBeforeReasoning(String question, boolean parallel) {
        sendOnBeforeReasoning(question, parallel);
    }

    public void sendOnBeforeReasoning(String question, boolean parallel) {
        sendToClient(emitter, "before-reasoning",
                "question", question,
                "parallel", parallel);
    }

    @Override
    public void onAfterReasoning(String question, ActingService.Plan plan, ReasoningJsonSchema.Result reason, boolean parallel) {
        sendOnAfterReasoning(question, plan, reason, parallel);
    }

    private void sendOnAfterReasoning(String question, ActingService.Plan plan,
                                      ReasoningJsonSchema.Result reason, boolean parallel) {
        sendToClient(emitter, "after-reasoning",
                "question", question,
                "parallel", parallel,
                "plan", plan);
    }

    @Override
    public void onReasoning(String question, ReasoningJsonSchema.Result result) {
        acceptReasoningEvent = true;
        sendOnReasoning(question, result);
        replayOnWebSearchRead();
    }

    private void sendOnReasoning(String question, ReasoningJsonSchema.Result result) {
        ReasoningJsonSchema.Result reasoning;
        if (result != null && result.isNeedSplitting()) {
            reasoning = result;
        } else {
            reasoning = new ReasoningJsonSchema.Result(false, Collections.singletonList(question));
        }
        sendToClient(emitter, "reasoning",
                "question", question,
                "reasoning", reasoning);
    }

    @Override
    public void onActing(String query, ActingService.Plan root, ActingService.Plan curr) {
        sendToClient(emitter, "acting",
                "curr", curr,
                "question", query,
                "root", root);
    }

    @Override
    public void onAfterWebSearchReduce(List<WebSearchResultVO> before, WebSearchResultVO after) {
        sendToClient(emitter, "after-websearch-reduce",
                "before", before,
                "after", after);
    }

    @Override
    public void onBeforeWebSearchReduce(List<WebSearchResultVO> before) {
        sendToClient(emitter, "before-websearch-reduce",
                "before", before);
    }

    @Override
    public void onToolCalls(Response<AiMessage> response) {
        if (debug) {
            AiMessage aiMessage = response.content();
            List<String> nameList = aiMessage.toolExecutionRequests().stream().map(ToolExecutionRequest::name).collect(Collectors.toList());
            List<Map<String, Object>> requestList = aiMessage.toolExecutionRequests().stream().map(e -> {
                Map<String, Object> request = new HashMap<>();
                request.put("name", e.name());
                request.put("arguments", e.arguments());
                request.put("id", e.id());
                return request;
            }).collect(Collectors.toList());
            sendToClient(emitter, "tool-calls",
                    "finishReason", response.finishReason().name(),
                    "nameList", nameList,
                    "requestList", requestList);
        }
    }

    @Override
    public void onAfterToolCalls(ToolExecutionRequest request, ToolExecutionResultMessage resultMessage, Throwable throwable) {
        if (debug) {
            sendToClient(emitter, "after-tool-calls",
                    "name", request.name(),
                    "arguments", request.arguments(),
                    "resultMessageText", resultMessage != null ? resultMessage.text() : null,
                    "id", request.id(),
                    "throwable", throwable != null ? throwable.toString() : null);
        }
    }

    public void sendComplete(Throwable t) {
        sendComplete = true;
        if (t != null) {
            onError(t, baseMessageIndex, addMessageCount, generateCount);
        } else {
            CompleteEvent completeEvent = this.completeEvent;
            if (completeEvent != null) {
                Response<AiMessage> response = completeEvent.response;
                FinishReason finishReason = response != null ? response.finishReason() : FinishReason.STOP;
                int baseMessageIndex = completeEvent.baseMessageIndex;
                int addMessageCount = completeEvent.addMessageCount;
                try {
                    complete(emitter, response, null, finishReason.name(), baseMessageIndex, addMessageCount);
                } catch (Exception e) {
                    if (log.isDebugEnabled()) {
                        log.debug("Error completing emitter {}", e.toString(), e);
                    }
                } finally {
                    this.close = true;
                }
            }
        }
    }

    @Override
    public void onComplete(Response<AiMessage> response, int baseMessageIndex, int addMessageCount, int generateCount) {
        this.completeEvent = new CompleteEvent(response, baseMessageIndex, addMessageCount, generateCount);
        if (sendComplete) {
            sendComplete(null);
        }
    }

    @Override
    public void onBeforeQuestionLlm(String question) {
        if (!acceptReasoningEvent && Boolean.TRUE.equals(websearch)) {
            // 开联网后，如果不需要拆分思考，需要拿用户的提问当作思考
            sendOnBeforeReasoning(question, true);
            sendOnReasoning(question, null);
            replayOnWebSearchRead();
            sendOnAfterReasoning(question, null, null, true);
        }
        onWebSearchReadList.clear();
        sendToClient(emitter, "before-question",
                "question", question);
    }

    private void replayOnWebSearchRead() {
        ArrayList<OnWebSearchRead> copyOnWebSearchReadList = new ArrayList<>(onWebSearchReadList);
        onWebSearchReadList.clear();
        for (OnWebSearchRead onWebSearchRead : copyOnWebSearchReadList) {
            sendOnWebSearchRead(onWebSearchRead.sourceEnum, onWebSearchRead.providerName, onWebSearchRead.question, onWebSearchRead.resultVO, onWebSearchRead.cost, true);
        }
    }

//    @Override
//    public void onFindJob(KnJobResp knJobResp, AiJobQuery parseQuery) {
//        List<KnJobVO> knList = knJobResp.getList();
//        AiJobQuery query = knJobResp.getQuery();
//
//        // 查询ES
//        Map<String, KnJobVO> knMap = knList.stream().collect(Collectors.toMap(KnVO::getId, e -> e, (o1, o2) -> o1));
//        List<Integer> jobIdList = knList.stream().map(KnVO::getId).map(Integer::valueOf).collect(Collectors.toList());
//        Map<String, AiJobSearchVO> jobSearchVOMap = jobSearchAdapter.convertJob(jobIdList);
//        List<KnEsJob<KnJobVO>> jobList = knList.stream().map(e -> KnEsJob.convert((KnJobVO) null, jobSearchVOMap.get(e.getId()))).collect(Collectors.toList());
//
//        sendToClient(emitter, "find-job",
//                "jobList", jobList,
//                "searchCostMs", knJobResp.getSearchCostMs(),
//                "embedCostMs", knJobResp.getEmbedCostMs(),
//                "query", query,
//                "parseQuery", parseQuery);
//    }

    @Override
    public void onKnowledge(List<List<QaKnVO>> knLibList, String query) {
        if (debug) {
            sendToClient(emitter, "kn-lib",
                    "knLibList", knLibList,
                    "question", query,
                    "queryRewrite", query
            );
        }
    }

    @Override
    public void onQuestionClassify(QuestionClassifyListVO questionClassify, String question, AiVariablesVO variables) {
        sendToClient(emitter, "question-classify",
                "classify", questionClassify.getClassifyResultList(),
                "question", question
        );
    }

    @Override
    public void onSystemMessage(String message, AiVariablesVO variables, String promptText) {
        if (debug) {
            sendToClient(emitter, "system-setting", "message", message, "variables", variables, "promptText", promptText);
        }
    }

    @Override
    public void onVariables(AiVariablesVO variables) {
        if (debug) {
            sendToClient(emitter, "init-variables", "variables", variables);
        }
    }

    @Override
    public <T> void onUserTrigger(UserTriggerEventEnum<T> eventName, T payload, long timestamp) {
        Objects.requireNonNull(eventName, "onUserTrigger#eventName");
        if (debug) {
            sendToClient(emitter, eventName.getCode(), "timestamp", timestamp);
        }
    }

    @Override
    public void onJsonSchema(Class<?> aiServiceClass, SystemMessage systemMessage, UserMessage userMessage, AiMessage response) {
        if (debug) {
            sendToClient(emitter, "json-schema",
                    "aiServiceClass", aiServiceClass.getSimpleName(),
                    "systemMessage", systemMessage == null ? null : systemMessage.text(),
                    "userMessage", AiUtil.userMessageToString(userMessage),
                    "response", response.text()
            );
        }
    }

    @Override
    public <T extends QaKnVO> void onRetriever(Map<String, List<T>> resultMap, String text, String beanName) {
        sendToClient(emitter, "retriever",
                "resultMap", resultMap,
                "text", text,
                "beanName", beanName
        );
    }

    protected void complete(Emitter emitter,
                            Response<AiMessage> response,
                            Throwable error,
                            String finishReason,
                            int baseMessageIndex,
                            int addMessageCount) {
        sendToClient(emitter, "complete",
                "finishReason", finishReason,
                "baseMessageIndex", baseMessageIndex,
                "addMessageCount", addMessageCount,
                "messageIndex", baseMessageIndex + addMessageCount);
        try {
            emitter.complete();
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.debug("Error completing emitter {}", e.toString(), e);
            }
        }
    }


    protected void beforeSendToClient(Emitter emitter, int id, String name, Map<Object, Object> map) {

    }

    protected void afterSendToClient(Emitter emitter, int id, String name, Map<Object, Object> map) {

    }

    public void sendToClient(String name, Object... data) {
        sendToClient(emitter, name, data);
    }

    private void sendToClient(Emitter emitter, String name, Object... data) {
        if (close) {
            return;
        }
        Map<Object, Object> map = new LinkedHashMap<>((data.length / 2) + 1);
        for (int i = 0; i < data.length; i += 2) {
            map.put(data[i], data[i + 1]);
        }
        int id = ++eventId;
        beforeSendToClient(emitter, id, name, map);
        try {
            emitter.send(String.valueOf(id), name, map);
        } catch (IOException | IllegalStateException e) {
            Throwable cause = e.getCause();
            if (cause == null) {
                cause = e;
            }
            Collection<String> exceptions = Arrays.asList("HttpMessageNotWritableException", "HttpMessageConversionException");
            if (exceptions.contains(cause.getClass().getSimpleName())) {
                log.warn("send fail {} ", e.toString(), e);
            } else {
                close = true;
            }
        } catch (Throwable e) {
            log.warn("send fail error  {} ", e.toString(), e);
        }
        afterSendToClient(emitter, id, name, map);
    }

    public interface Emitter {
        void send(String id, String name, Object data) throws IOException, IllegalStateException;

        void complete();
    }

    static class OnWebSearchRead {
        final AiWebSearchSourceEnum sourceEnum;
        final String providerName;
        final String question;
        final WebSearchResultVO resultVO;
        final long cost;

        OnWebSearchRead(AiWebSearchSourceEnum sourceEnum, String providerName, String question, WebSearchResultVO resultVO, long cost) {
            this.sourceEnum = sourceEnum;
            this.providerName = providerName;
            this.question = question;
            this.resultVO = resultVO;
            this.cost = cost;
        }
    }

    static class CompleteEvent {
        final Response<AiMessage> response;
        final int baseMessageIndex;
        final int addMessageCount;
        final int generateCount;

        public CompleteEvent(Response<AiMessage> response, int baseMessageIndex, int addMessageCount, int generateCount) {
            this.response = response;
            this.baseMessageIndex = baseMessageIndex;
            this.addMessageCount = addMessageCount;
            this.generateCount = generateCount;
        }
    }


}
