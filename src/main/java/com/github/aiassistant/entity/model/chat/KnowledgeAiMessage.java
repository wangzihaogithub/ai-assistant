package com.github.aiassistant.entity.model.chat;

import dev.langchain4j.data.message.AiMessage;

public class KnowledgeAiMessage extends AiMessage {
    private final KnowledgeTextContent knowledgeTextContent;

    public KnowledgeAiMessage(KnowledgeTextContent content) {
        super(content.text());
        this.knowledgeTextContent = content;
    }

    public KnowledgeTextContent getKnowledgeTextContent() {
        return knowledgeTextContent;
    }
}
