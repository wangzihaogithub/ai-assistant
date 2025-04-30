package com.github.aiassistant.entity.model.chat;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;

import java.util.List;

public class ThinkingAiMessage extends AiMessage {

    public ThinkingAiMessage(String content) {
        super(content);
    }

    public ThinkingAiMessage(List<ToolExecutionRequest> toolExecutionRequests) {
        super(toolExecutionRequests);
    }

    public ThinkingAiMessage(String text, List<ToolExecutionRequest> toolExecutionRequests) {
        super(text, toolExecutionRequests);
    }
}
