package com.github.aiassistant.service.text.tools;

public enum ToolInvokeEnum {
    PARAM_VALID,
    METHOD;

    public boolean isValid() {
        return this == PARAM_VALID;
    }
}