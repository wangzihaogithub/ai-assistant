package com.github.aiassistant.entity.model.langchain4j;

import dev.langchain4j.data.message.UserMessage;

public class LangChainUserMessage extends UserMessage {
    public LangChainUserMessage(String text) {
        super(text);
    }
}
