package com.github.aiassistant.serviceintercept;

import com.github.aiassistant.entity.model.chat.AiVariables;
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
            AiVariables variables,
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
            AiVariables variables,
            Boolean websearch,
            Boolean reasoning,
            ChatStreamingResponseHandler responseHandler,
            List<ChatMessage> historyList,
            String question,
            String lastQuestion) {
        return null;
    }
}
