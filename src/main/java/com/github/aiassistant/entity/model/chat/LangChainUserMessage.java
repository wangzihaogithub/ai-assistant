package com.github.aiassistant.entity.model.chat;

import dev.langchain4j.data.message.UserMessage;

public class LangChainUserMessage extends UserMessage {
    public LangChainUserMessage(String text) {
        super(text);
    }
}
