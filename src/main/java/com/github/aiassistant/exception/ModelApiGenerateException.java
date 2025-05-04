package com.github.aiassistant.exception;

import dev.langchain4j.data.message.ChatMessage;

import java.util.List;

/**
 * 供应商的模型API生成失败
 */
public class ModelApiGenerateException extends AiAssistantException {
    private final String modelName;
    private final Object memoryId;
    private final List<ChatMessage> messageList;

    public ModelApiGenerateException(String message, Throwable cause,
                                     String modelName,
                                     Object memoryId,
                                     List<ChatMessage> messageList) {
        super(message, cause);
        this.modelName = modelName;
        this.memoryId = memoryId;
        this.messageList = messageList;
    }

    public String getModelName() {
        return modelName;
    }

    public Object getMemoryId() {
        return memoryId;
    }

    public List<ChatMessage> getMessageList() {
        return messageList;
    }

}
