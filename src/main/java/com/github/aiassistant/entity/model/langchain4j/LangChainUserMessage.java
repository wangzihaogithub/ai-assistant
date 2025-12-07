package com.github.aiassistant.entity.model.langchain4j;

import dev.langchain4j.data.message.Content;
import dev.langchain4j.data.message.UserMessage;

import java.util.List;

public class LangChainUserMessage extends UserMessage {
    public LangChainUserMessage(String text) {
        super(text);
    }

    public LangChainUserMessage(String name, List<Content> contents) {
        super(name, contents);
    }

    public LangChainUserMessage(List<Content> contents) {
        super(contents);
    }

    public static LangChainUserMessage convert(UserMessage userMessage) {
        String name = userMessage.name();
        List<Content> contents = userMessage.contents();
        if (name == null || name.trim().isEmpty()) {
            return new LangChainUserMessage(contents);
        } else {
            return new LangChainUserMessage(name, contents);
        }
    }

}
