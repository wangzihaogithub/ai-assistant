package com.github.aiassistant.exception;

import com.github.aiassistant.service.text.AssistantConfig;

/**
 * 智能体配置出现错误
 */
public class AssistantConfigException extends AiAssistantException {
    private final AssistantConfig assistantConfig;

    public AssistantConfigException(String message, Throwable cause,
                                    AssistantConfig assistantConfig) {
        super(message, cause);
        this.assistantConfig = assistantConfig;
    }

    public AssistantConfig getAssistantConfig() {
        return assistantConfig;
    }
}
