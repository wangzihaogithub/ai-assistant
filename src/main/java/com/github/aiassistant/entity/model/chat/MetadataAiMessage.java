package com.github.aiassistant.entity.model.chat;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.output.Response;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class MetadataAiMessage extends AiMessage {
    public static final String METADATA_KEY_MEMORY_STRING = "memoryString";

    private final String openAiRequestId;
    private final String memoryString;

    public MetadataAiMessage(String text, String memoryString, String openAiRequestId) {
        super(text);
        this.openAiRequestId = openAiRequestId;
        this.memoryString = memoryString;
    }

    public MetadataAiMessage(List<ToolExecutionRequest> toolExecutionRequests, String memoryString, String openAiRequestId) {
        super(toolExecutionRequests);
        this.openAiRequestId = openAiRequestId;
        this.memoryString = memoryString;
    }

    public MetadataAiMessage(String text, List<ToolExecutionRequest> toolExecutionRequests, String memoryString, String openAiRequestId) {
        super(text, toolExecutionRequests);
        this.openAiRequestId = openAiRequestId;
        this.memoryString = memoryString;
    }

    public static MetadataAiMessage convert(Response<AiMessage> response) {
        Map<String, Object> metadata = response.metadata();
        String openAiRequestId = null;
        String memoryString = null;
        if (metadata != null) {
            openAiRequestId = Objects.toString(metadata.get("id"), null);
            memoryString = Objects.toString(metadata.get(METADATA_KEY_MEMORY_STRING), null);
        }
        AiMessage m = response.content();
        if (m.text() != null && m.hasToolExecutionRequests()) {
            return new MetadataAiMessage(m.text(), m.toolExecutionRequests(), memoryString, openAiRequestId);
        } else if (m.text() != null) {
            return new MetadataAiMessage(m.text(), memoryString, openAiRequestId);
        } else {
            return new MetadataAiMessage(m.toolExecutionRequests(), memoryString, openAiRequestId);
        }
    }

    public String getOpenAiRequestId() {
        return openAiRequestId;
    }

    public String getMemoryString() {
        return memoryString;
    }
}
