package com.github.aiassistant.entity.model.chat;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.ToolExecutionResultMessage;

public class UrlReadToolExecutionResultMessage extends ToolExecutionResultMessage {
    private final String url;

    public UrlReadToolExecutionResultMessage(ToolExecutionRequest request, String text, String url) {
        super(request.id(), request.name(), text);
        this.url = url;
    }

    public String getUrl() {
        return url;
    }
}
