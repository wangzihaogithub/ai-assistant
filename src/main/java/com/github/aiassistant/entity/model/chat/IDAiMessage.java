package com.github.aiassistant.entity.model.chat;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.output.Response;

import java.util.List;
import java.util.Objects;

public class IDAiMessage extends AiMessage {

    private final String id;

    public IDAiMessage(String text, String id) {
        super(text);
        this.id = id;
    }

    public IDAiMessage(List<ToolExecutionRequest> toolExecutionRequests, String id) {
        super(toolExecutionRequests);
        this.id = id;
    }

    public IDAiMessage(String text, List<ToolExecutionRequest> toolExecutionRequests, String id) {
        super(text, toolExecutionRequests);
        this.id = id;
    }

    public static IDAiMessage convert(Response<AiMessage> response) {
        String id = Objects.toString(response.metadata().get("id"), null);
        AiMessage m = response.content();
        if (m.text() != null && m.hasToolExecutionRequests()) {
            return new IDAiMessage(m.text(), m.toolExecutionRequests(), id);
        } else if (m.text() != null) {
            return new IDAiMessage(m.text(), id);
        } else {
            return new IDAiMessage(m.toolExecutionRequests(), id);
        }
    }

    public String getId() {
        return id;
    }
}
