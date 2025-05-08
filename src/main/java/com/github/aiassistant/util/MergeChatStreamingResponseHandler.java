package com.github.aiassistant.util;

import com.github.aiassistant.entity.model.chat.AiVariablesVO;
import com.github.aiassistant.entity.model.chat.QaKnVO;
import com.github.aiassistant.entity.model.chat.QuestionClassifyListVO;
import com.github.aiassistant.entity.model.chat.WebSearchResultVO;
import com.github.aiassistant.enums.AiWebSearchSourceEnum;
import com.github.aiassistant.enums.UserTriggerEventEnum;
import com.github.aiassistant.service.jsonschema.ReasoningJsonSchema;
import com.github.aiassistant.service.text.ChatStreamingResponseHandler;
import com.github.aiassistant.service.text.acting.ActingService;
import com.github.aiassistant.service.text.sseemitter.AiMessageString;
import com.github.aiassistant.service.text.sseemitter.SseHttpResponse;
import com.github.aiassistant.service.text.tools.functioncall.UrlReadTools;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.output.Response;

import java.util.List;
import java.util.Map;

public class MergeChatStreamingResponseHandler implements ChatStreamingResponseHandler {
    private final List<ChatStreamingResponseHandler> list;
    private final ChatStreamingResponseHandler userHandler;

    public MergeChatStreamingResponseHandler(List<ChatStreamingResponseHandler> list, ChatStreamingResponseHandler userHandler) {
        this.list = list;
        this.userHandler = userHandler;
    }

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
    public void onModelThinkingToken(String thinkingToken) {
        for (ChatStreamingResponseHandler h : list) {
            h.onModelThinkingToken(thinkingToken);
        }
    }

    @Override
    public void onAfterModelThinking(Response<AiMessage>  thinkingResponse) {
        for (ChatStreamingResponseHandler h : list) {
            h.onAfterModelThinking(thinkingResponse);
        }
    }

    @Override
    public void onBeforeModelThinking() {
        for (ChatStreamingResponseHandler h : list) {
            h.onBeforeModelThinking();
        }
    }

    @Override
    public void onBlacklistQuestion(SseHttpResponse response, String question, QuestionClassifyListVO classifyListVO) {
        for (ChatStreamingResponseHandler h : list) {
            h.onBlacklistQuestion(response, question, classifyListVO);
        }
    }

    @Override
    public void onToken(AiMessageString token, int baseMessageIndex, int addMessageCount) {
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
    public void onQuestionClassify(QuestionClassifyListVO questionClassify, String question, AiVariablesVO variables) {
        for (ChatStreamingResponseHandler h : list) {
            h.onQuestionClassify(questionClassify, question, variables);
        }
    }

    @Override
    public void onSystemMessage(String message, AiVariablesVO variables, String promptText) {
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
    public void onVariables(AiVariablesVO variables) {
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
    public void onBeforeReasoningAndActing(boolean reasoningAndActing) {
        for (ChatStreamingResponseHandler h : list) {
            h.onBeforeReasoningAndActing(reasoningAndActing);
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
}
