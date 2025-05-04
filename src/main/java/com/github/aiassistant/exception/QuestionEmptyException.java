package com.github.aiassistant.exception;

import dev.langchain4j.data.message.ChatMessage;

import java.util.List;

/**
 * 提问不能为空
 */
public class QuestionEmptyException extends AiAssistantException {
    private final List<ChatMessage> historyList;

    public QuestionEmptyException(String message, List<ChatMessage> historyList) {
        super(message);
        this.historyList = historyList;
    }

    public List<ChatMessage> getHistoryList() {
        return historyList;
    }
}
