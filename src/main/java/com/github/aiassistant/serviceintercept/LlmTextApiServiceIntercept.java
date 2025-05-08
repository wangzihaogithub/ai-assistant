package com.github.aiassistant.serviceintercept;

import com.github.aiassistant.entity.model.chat.AiVariablesVO;
import com.github.aiassistant.entity.model.chat.MemoryIdVO;
import com.github.aiassistant.entity.model.chat.QuestionClassifyListVO;
import com.github.aiassistant.entity.model.user.AiAccessUserVO;
import com.github.aiassistant.service.text.ChatStreamingResponseHandler;
import com.github.aiassistant.service.text.FunctionCallStreamingResponseHandler;
import dev.langchain4j.data.message.ChatMessage;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public interface LlmTextApiServiceIntercept extends ServiceIntercept {
    default Function<FunctionCallStreamingResponseHandler, CompletableFuture<Void>> interceptQuestion(
            AiAccessUserVO user,
            MemoryIdVO memoryId,
            QuestionClassifyListVO classifyListVO,
            AiVariablesVO variables,
            Boolean websearch,
            Boolean reasoning,
            ChatStreamingResponseHandler responseHandler,
            List<ChatMessage> historyList,
            String question,
            String lastQuestion) {
        return null;
    }

    default Function<FunctionCallStreamingResponseHandler, CompletableFuture<Void>> interceptRepository(
            AiAccessUserVO user,
            MemoryIdVO memoryId,
            QuestionClassifyListVO classifyListVO,
            AiVariablesVO variables,
            Boolean websearch,
            Boolean reasoning,
            ChatStreamingResponseHandler responseHandler,
            List<ChatMessage> historyList,
            String question,
            String lastQuestion) {
        return null;
    }

    default List<ChatMessage> interceptHistoryList(List<ChatMessage> historyList,
                                                   AiAccessUserVO user,
                                                   MemoryIdVO memoryId,
                                                   Boolean websearch,
                                                   Boolean reasoning,
                                                   ChatStreamingResponseHandler responseHandler,
                                                   String question) {
        return historyList;
    }
}
