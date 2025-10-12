package com.github.aiassistant.entity.model.chat;

import java.io.Serializable;

public class ToolResponseVO implements Serializable {
    private String requestId;
    private String toolName;

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
}
