package com.github.aiassistant.entity.model.langchain4j;

import com.github.aiassistant.util.AiUtil;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.output.TokenUsage;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class MetadataAiMessage extends AiMessage {
    public static final String METADATA_KEY_MEMORY_STRING = "memoryString";

    private final Response<AiMessage> response;

    public MetadataAiMessage(String text, Response<AiMessage> response) {
        super(text);
        this.response = response;
    }

    public MetadataAiMessage(List<ToolExecutionRequest> toolExecutionRequests, Response<AiMessage> response) {
        super(toolExecutionRequests);
        this.response = response;
    }

    public MetadataAiMessage(String text, List<ToolExecutionRequest> toolExecutionRequests, Response<AiMessage> response) {
        super(text, toolExecutionRequests);
        this.response = response;
    }

    public static MetadataAiMessage convert(Response<AiMessage> response) {
        AiMessage m = response.content();
        if (m.text() != null && m.hasToolExecutionRequests()) {
            return new MetadataAiMessage(m.text(), m.toolExecutionRequests(), response);
        } else if (m.text() != null) {
            return new MetadataAiMessage(m.text(), response);
        } else {
            return new MetadataAiMessage(m.toolExecutionRequests(), response);
        }
    }

    public String getOpenAiRequestId() {
        Map<String, Object> metadata = response.metadata();
        String openAiRequestId = null;
        if (metadata != null) {
            openAiRequestId = Objects.toString(metadata.get("id"), null);
        }
        return openAiRequestId;
    }

    public String getMemoryString() {
        Map<String, Object> metadata = response.metadata();
        String memoryString = null;
        if (metadata != null) {
            memoryString = Objects.toString(metadata.get(METADATA_KEY_MEMORY_STRING), null);
        }
        return memoryString;
    }

    public int getTotalTokenCount() {
        TokenUsage tokenUsage = response.tokenUsage();
        int ct;
        if (tokenUsage != null) {
            ct = tokenUsage.totalTokenCount();
        } else {
            ct = 0;
        }
        return ct;
    }

    public int getInputTokenCount() {
        TokenUsage tokenUsage = response.tokenUsage();
        int ct;
        if (tokenUsage != null) {
            ct = tokenUsage.inputTokenCount();
        } else {
            ct = 0;
        }
        return ct;
    }

    public int getOutputTokenCount() {
        TokenUsage tokenUsage = response.tokenUsage();
        int ct;
        if (tokenUsage != null) {
            ct = tokenUsage.outputTokenCount();
        } else {
            ct = 0;
        }
        return ct;
    }

    public boolean isTypeThinkingAiMessage() {
        return AiUtil.isTypeThinkingAiMessage(response.content());
    }

    public Response<AiMessage> getResponse() {
        return response;
    }
}
