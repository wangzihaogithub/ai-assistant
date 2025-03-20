package com.github.aiassistant.entity.model.chat;

import dev.langchain4j.data.message.UserMessage;

public class FewshotUserMessage extends UserMessage {
    public FewshotUserMessage(String text) {
        super(text);
    }
}
