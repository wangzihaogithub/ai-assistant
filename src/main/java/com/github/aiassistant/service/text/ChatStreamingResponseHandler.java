package com.github.aiassistant.service.text;

import com.github.aiassistant.entity.model.chat.*;
import com.github.aiassistant.entity.model.user.AiAccessUserVO;
import com.github.aiassistant.enums.AiWebSearchSourceEnum;
import com.github.aiassistant.enums.UserTriggerEventEnum;
import com.github.aiassistant.service.jsonschema.ReasoningJsonSchema;
import com.github.aiassistant.service.text.acting.ActingService;
import com.github.aiassistant.service.text.embedding.KnnResponseListenerFuture;
import com.github.aiassistant.service.text.sseemitter.AiMessageString;
import com.github.aiassistant.service.text.sseemitter.SseHttpResponse;
import com.github.aiassistant.service.text.tools.WebSearchService;
import com.github.aiassistant.service.text.tools.functioncall.UrlReadTools;
import com.github.aiassistant.util.ResponseHandlerWebSearchEventListener;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.*;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.model.openai.AudioChunk;
import dev.langchain4j.model.output.Response;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 聊天流定义的事件接口
 */
public interface ChatStreamingResponseHandler {

    ChatStreamingResponseHandler EMPTY = new ChatStreamingResponseHandler() {
    };

    default ChatStreamingResponseHandler getUserHandler() {
        return this;
    }

    /**
     * 构建请求前调用,完成后必须调用 {@link RequestBuilder#complete()} 方法
     *
     * @param requestBuilder 请求构建器
     * @param historyList    历史消息列表
     * @param user           当前用户
     * @param variables      变量
     * @param memoryId       内存ID
     * @param websearch      是否开启联网搜索
     * @param reasoning      是否开启推理
     * @param question       问题
     */
    default void onBeforeBuildRequest(RequestBuilder requestBuilder, List<ChatMessage> historyList, AiAccessUserVO user, AiVariablesVO variables, MemoryIdVO memoryId, Boolean websearch, Boolean reasoning, String question) {
        requestBuilder.complete();
    }

    /**
     * 对大模型的返回结果进行追问,如果需要追问,则返回追问的问题列表,否则返回空列表
     *
     * @param chatMemory 记忆
     * @param response   大模型返回结果
     * @return 追问的问题列表，如果需要追问,则返回追问的问题列表,否则返回空列表
     */
    default CompletableFuture<List<ChatMessage>> onBeforeCompleteFurtherQuestioning(ChatMemory chatMemory, Response<AiMessage> response) {
        return CompletableFuture.completedFuture(Collections.emptyList());
    }

    default AiVariablesVO onAfterSelectVariables(AiVariablesVO variables,
                                                 AiAccessUserVO currentUser, List<ChatMessage> historyList,
                                                 String lastQuestion, MemoryIdVO memoryId, Boolean websearch) {
//        // 员工
//        setterEmployees(variables.getEmployees()); // 学生
//        setterStudent(variables.getStudent(), currentUser);      // 竞争对手
//        setterRival(variables.getRival());
//        return variables;
        return variables;
    }

    default void onAudio(AudioChunk audioChunk) {

    }

    default void onBeforeModelThinking() {

    }

    default void onModelThinkingToken(String thinkingToken) {
    }

    default void onAfterModelThinking(Response<AiMessage> thinkingResponse) {

    }

    default void onKnnSearch(KnnResponseListenerFuture<? extends KnVO> future) {
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

    default void onQuestionClassify(QuestionClassifyListVO questionClassify, String question, AiVariablesVO variables) {
    }

    default void onSystemMessage(String message, AiVariablesVO variables, String promptText) {

    }
//
//    default void onFindJob(KnJobResp knJobResp, AiJobQuery query) {
//
//    }

    default void onVariables(AiVariablesVO variables) {

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

    default List<ChatMessage> onAfterSelectHistoryList(List<ChatMessage> repositoryHistoryList, AiAccessUserVO user, MemoryIdVO memoryId, Boolean websearch, Boolean reasoning, String question) {
        return repositoryHistoryList;
    }
}