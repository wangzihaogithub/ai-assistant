package com.github.aiassistant.entity.model.chat;

import dev.langchain4j.data.message.AiMessage;

public class FewshotAiMessage extends AiMessage {
    public FewshotAiMessage(String text) {
        super(text);
    }
}
