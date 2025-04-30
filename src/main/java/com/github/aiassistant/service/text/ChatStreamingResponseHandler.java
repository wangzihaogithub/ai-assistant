package com.github.aiassistant.service.text;

import com.github.aiassistant.entity.model.chat.AiVariables;
import com.github.aiassistant.entity.model.chat.QaKnVO;
import com.github.aiassistant.entity.model.chat.QuestionClassifyListVO;
import com.github.aiassistant.entity.model.chat.WebSearchResultVO;
import com.github.aiassistant.enums.AiWebSearchSourceEnum;
import com.github.aiassistant.enums.UserTriggerEventEnum;
import com.github.aiassistant.service.jsonschema.ReasoningJsonSchema;
import com.github.aiassistant.service.text.acting.ActingService;
import com.github.aiassistant.service.text.sseemitter.AiMessageString;
import com.github.aiassistant.service.text.sseemitter.SseHttpResponse;
import com.github.aiassistant.service.text.tools.WebSearchService;
import com.github.aiassistant.service.text.tools.functioncall.UrlReadTools;
import com.github.aiassistant.util.ResponseHandlerWebSearchEventListener;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.output.Response;

import java.util.List;
import java.util.Map;

/**
 * 聊天流定义的事件接口
 */
public interface ChatStreamingResponseHandler {

    ChatStreamingResponseHandler EMPTY = new ChatStreamingResponseHandler() {
    };

    default ChatStreamingResponseHandler getUserHandler() {
        return this;
    }

    default void onBeforeModelThinking() {

    }

    default void onModelThinkingToken(String thinkingToken) {
    }

    default void onAfterModelThinking(Response<AiMessage> thinkingResponse) {

    }

    default void onTokenBegin(int baseMessageIndex, int addMessageCount, int generateCount) {
    }

    default void onToken(AiMessageString token, int baseMessageIndex, int addMessageCount) {
    }

    default void onTokenEnd(Response<AiMessage> response, int baseMessageIndex, int addMessageCount, int generateCount) {
    }

    default void onComplete(Response<AiMessage> response, int baseMessageIndex, int addMessageCount, int generateCount) {
    }

    default void onError(Throwable error, int baseMessageIndex, int addMessageCount, int generateCount) {
    }

    default void onToolCalls(Response<AiMessage> response) {
    }

    default void onBlacklistQuestion(SseHttpResponse response, String question, QuestionClassifyListVO classifyListVO) {
    }

    default <T> void onUserTrigger(UserTriggerEventEnum<T> triggerEventEnum, T payload, long timestamp) {
//            @Override
//            public void onFindJob(KnJobResp knJobResp, AiJobQuery query) {
//                for (ChatStreamingResponseHandler h : list) {
//                    h.onFindJob(knJobResp, query);
//                }
//            }
    }

    default void onAfterToolCalls(ToolExecutionRequest request, ToolExecutionResultMessage resultMessage, Throwable throwable) {
    }

    default void onKnowledge(List<List<QaKnVO>> knLibList, String question) {
    }

    default void onQuestionClassify(QuestionClassifyListVO questionClassify, String question, AiVariables variables) {
    }

    default void onSystemMessage(String message, AiVariables variables, String promptText) {

    }
//
//    default void onFindJob(KnJobResp knJobResp, AiJobQuery query) {
//
//    }

    default void onVariables(AiVariables variables) {

    }

    default void onJsonSchema(Class<?> aiServiceClass, SystemMessage systemMessage, UserMessage userMessage, AiMessage response) {

    }

    default void onJsonSchemaToolCalls(Response<AiMessage> response) {
    }

    default void onBeforeQuestionLlm(String question) {
    }

    default void onBeforeReasoning(String question, boolean parallel) {

    }

    default void onBeforeReasoningAndActing(boolean reasoningAndActing) {

    }

    default void onReasoning(String question, ReasoningJsonSchema.Result result) {

    }

    default void onBeforeWebSearchReduce(List<WebSearchResultVO> before) {

    }

    default void onAfterWebSearchReduce(List<WebSearchResultVO> before, WebSearchResultVO after) {

    }

    default void onAfterReasoning(String question, ActingService.Plan plan, ReasoningJsonSchema.Result reason, boolean parallel) {

    }

    default void onActing(String question, ActingService.Plan root, ActingService.Plan curr) {

    }

    default <T extends QaKnVO> void onRetriever(Map<String, List<T>> resultMap, String text, String beanName) {

    }

    default void beforeWebSearch(AiWebSearchSourceEnum sourceEnum, String providerName, String question) {

    }

    default void afterWebSearch(AiWebSearchSourceEnum sourceEnum, String providerName, String question, WebSearchResultVO resultVO, long cost) {

    }

    default void beforeUrlRead(AiWebSearchSourceEnum sourceEnum, String providerName, String question, UrlReadTools urlReadTools, WebSearchResultVO.Row row) {

    }

    default void afterUrlRead(AiWebSearchSourceEnum sourceEnum, String providerName, String question, UrlReadTools urlReadTools, WebSearchResultVO.Row row, String content, String merge, long cost) {

    }

    default WebSearchService.EventListener adapterWebSearch(AiWebSearchSourceEnum sourceEnum) {
        return new ResponseHandlerWebSearchEventListener(this, sourceEnum);
    }

}