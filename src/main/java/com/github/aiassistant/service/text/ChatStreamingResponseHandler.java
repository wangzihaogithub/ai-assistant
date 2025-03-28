package com.github.aiassistant.service.text;

import com.github.aiassistant.entity.model.chat.AiVariables;
import com.github.aiassistant.entity.model.chat.QaKnVO;
import com.github.aiassistant.entity.model.chat.QuestionClassifyListVO;
import com.github.aiassistant.entity.model.chat.WebSearchResultVO;
import com.github.aiassistant.enums.AiWebSearchSourceEnum;
import com.github.aiassistant.enums.UserTriggerEventEnum;
import com.github.aiassistant.service.jsonschema.ReasoningJsonSchema;
import com.github.aiassistant.service.text.acting.ActingService;
import com.github.aiassistant.service.text.sseemitter.SseHttpResponse;
import com.github.aiassistant.service.text.tools.WebSearchService;
import com.github.aiassistant.service.text.tools.functioncall.UrlReadTools;
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

    public static ChatStreamingResponseHandler merge(List<ChatStreamingResponseHandler> list, ChatStreamingResponseHandler userHandler) {
        return new ChatStreamingResponseHandler() {
            @Override
            public ChatStreamingResponseHandler getUserHandler() {
                return userHandler;
            }

            @Override
            public void onTokenBegin(int baseMessageIndex, int addMessageCount, int generateCount) {
                for (ChatStreamingResponseHandler h : list) {
                    h.onTokenBegin(baseMessageIndex, addMessageCount, generateCount);
                }
            }

            @Override
            public void onBlacklistQuestion(SseHttpResponse response, String question, QuestionClassifyListVO classifyListVO) {
                for (ChatStreamingResponseHandler h : list) {
                    h.onBlacklistQuestion(response, question, classifyListVO);
                }
            }

            @Override
            public void onToken(String token, int baseMessageIndex, int addMessageCount) {
                for (ChatStreamingResponseHandler h : list) {
                    h.onToken(token, baseMessageIndex, addMessageCount);
                }
            }

            @Override
            public void onTokenEnd(Response<AiMessage> response, int baseMessageIndex, int addMessageCount, int generateCount) {
                for (ChatStreamingResponseHandler h : list) {
                    h.onTokenEnd(response, baseMessageIndex, addMessageCount, generateCount);
                }
            }

            @Override
            public void onComplete(Response<AiMessage> response, int baseMessageIndex, int addMessageCount, int generateCount) {
                for (ChatStreamingResponseHandler h : list) {
                    h.onComplete(response, baseMessageIndex, addMessageCount, generateCount);
                }
            }

            @Override
            public void onError(Throwable error, int baseMessageIndex, int addMessageCount, int generateCount) {
                for (ChatStreamingResponseHandler h : list) {
                    h.onError(error, baseMessageIndex, addMessageCount, generateCount);
                }
            }

            @Override
            public void onToolCalls(Response<AiMessage> response) {
                for (ChatStreamingResponseHandler h : list) {
                    h.onToolCalls(response);
                }
            }

            @Override
            public void onKnowledge(List<List<QaKnVO>> knLibList, String question) {
                for (ChatStreamingResponseHandler h : list) {
                    h.onKnowledge(knLibList, question);
                }
            }

            @Override
            public void onQuestionClassify(QuestionClassifyListVO questionClassify, String question, AiVariables variables) {
                for (ChatStreamingResponseHandler h : list) {
                    h.onQuestionClassify(questionClassify, question, variables);
                }
            }

            @Override
            public void onSystemMessage(String message, AiVariables variables, String promptText) {
                for (ChatStreamingResponseHandler h : list) {
                    h.onSystemMessage(message, variables, promptText);
                }
            }

            @Override
            public void onAfterToolCalls(ToolExecutionRequest request, ToolExecutionResultMessage resultMessage, Throwable throwable) {
                for (ChatStreamingResponseHandler h : list) {
                    h.onAfterToolCalls(request, resultMessage, throwable);
                }
            }

            @Override
            public <T> void onUserTrigger(UserTriggerEventEnum<T> triggerEventEnum, T payload, long timestamp) {
                for (ChatStreamingResponseHandler h : list) {
                    h.onUserTrigger(triggerEventEnum, payload, timestamp);
                }
            }

            @Override
            public void onVariables(AiVariables variables) {
                for (ChatStreamingResponseHandler h : list) {
                    h.onVariables(variables);
                }
            }

            @Override
            public void onJsonSchema(Class<?> aiServiceClass, SystemMessage systemMessage, UserMessage userMessage, AiMessage response) {
                for (ChatStreamingResponseHandler h : list) {
                    h.onJsonSchema(aiServiceClass, systemMessage, userMessage, response);
                }
            }

            @Override
            public void onJsonSchemaToolCalls(Response<AiMessage> response) {
                for (ChatStreamingResponseHandler h : list) {
                    h.onJsonSchemaToolCalls(response);
                }
            }

            @Override
            public void afterUrlRead(AiWebSearchSourceEnum sourceEnum, String providerName, String question, UrlReadTools urlReadTools, WebSearchResultVO.Row row, String content, String merge, long cost) {
                for (ChatStreamingResponseHandler h : list) {
                    h.afterUrlRead(sourceEnum, providerName, question, urlReadTools, row, content, merge, cost);
                }
            }

            @Override
            public void afterWebSearch(AiWebSearchSourceEnum sourceEnum, String providerName, String question, WebSearchResultVO resultVO, long cost) {
                for (ChatStreamingResponseHandler h : list) {
                    h.afterWebSearch(sourceEnum, providerName, question, resultVO, cost);
                }
            }

            @Override
            public void beforeUrlRead(AiWebSearchSourceEnum sourceEnum, String providerName, String question, UrlReadTools urlReadTools, WebSearchResultVO.Row row) {
                for (ChatStreamingResponseHandler h : list) {
                    h.beforeUrlRead(sourceEnum, providerName, question, urlReadTools, row);
                }
            }

            @Override
            public void beforeWebSearch(AiWebSearchSourceEnum sourceEnum, String providerName, String question) {
                for (ChatStreamingResponseHandler h : list) {
                    h.beforeWebSearch(sourceEnum, providerName, question);
                }
            }

            @Override
            public void onReasoning(String question, ReasoningJsonSchema.Result result) {
                for (ChatStreamingResponseHandler h : list) {
                    h.onReasoning(question, result);
                }
            }

            @Override
            public void onActing(String question, ActingService.Plan root, ActingService.Plan curr) {
                for (ChatStreamingResponseHandler h : list) {
                    h.onActing(question, root, curr);
                }
            }

            @Override
            public void onAfterWebSearchReduce(List<WebSearchResultVO> before, WebSearchResultVO after) {
                for (ChatStreamingResponseHandler h : list) {
                    h.onAfterWebSearchReduce(before, after);
                }
            }

            @Override
            public void onBeforeWebSearchReduce(List<WebSearchResultVO> before) {
                for (ChatStreamingResponseHandler h : list) {
                    h.onBeforeWebSearchReduce(before);
                }
            }

            @Override
            public <T extends QaKnVO> void onRetriever(Map<String, List<T>> resultMap, String text, String beanName) {
                for (ChatStreamingResponseHandler h : list) {
                    h.onRetriever(resultMap, text, beanName);
                }
            }

            @Override
            public void onBeforeReasoning(String question, boolean parallel) {
                for (ChatStreamingResponseHandler h : list) {
                    h.onBeforeReasoning(question, parallel);
                }
            }

            @Override
            public void onAfterReasoning(String question, ActingService.Plan plan, ReasoningJsonSchema.Result reason, boolean parallel) {
                for (ChatStreamingResponseHandler h : list) {
                    h.onAfterReasoning(question, plan, reason, parallel);
                }
            }

            @Override
            public void onBeforeQuestionLlm(String question) {
                for (ChatStreamingResponseHandler h : list) {
                    h.onBeforeQuestionLlm(question);
                }
            }
        };
    }

    default ChatStreamingResponseHandler getUserHandler() {
        return this;
    }

    public default WebSearchService.EventListener adapterWebSearch(AiWebSearchSourceEnum sourceEnum) {
        ChatStreamingResponseHandler h = this;
        return new WebSearchService.EventListener() {
            @Override
            public void beforeWebSearch(String providerName, String question) {
                h.beforeWebSearch(sourceEnum, providerName, question);
            }

            @Override
            public void afterWebSearch(String providerName, String question, WebSearchResultVO resultVO, long cost) {
                h.afterWebSearch(sourceEnum, providerName, question, resultVO, cost);
            }

            @Override
            public void beforeUrlRead(String providerName, String question, UrlReadTools urlReadTools, WebSearchResultVO.Row row) {
                h.beforeUrlRead(sourceEnum, providerName, question, urlReadTools, row);
            }

            @Override
            public void afterUrlRead(String providerName, String question, UrlReadTools urlReadTools, WebSearchResultVO.Row row, String content, String merge, long cost) {
                h.afterUrlRead(sourceEnum, providerName, question, urlReadTools, row, content, merge, cost);
            }
        };
    }

    default void onTokenBegin(int baseMessageIndex, int addMessageCount, int generateCount) {
    }

    default void onToken(String token, int baseMessageIndex, int addMessageCount) {
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
}