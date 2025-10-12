package com.github.aiassistant.entity.model.chat;

import java.io.Serializable;

public class ToolRequestVO implements Serializable {
    private String requestId;
    private String toolName;
    private String arguments;

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getToolName() {
        return toolName;
    }

    public void setToolName(String toolName) {
        this.toolName = toolName;
    }

    public String getArguments() {
        return arguments;
    }

    public void setArguments(String arguments) {
        this.arguments = arguments;
    }
}