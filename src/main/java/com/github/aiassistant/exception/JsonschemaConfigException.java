package com.github.aiassistant.exception;

/**
 * json模型配置出现错误
 */
public class JsonschemaConfigException extends AiAssistantException {
    private final Class<?> aiServiceClass;

    public JsonschemaConfigException(String message, Throwable cause, Class<?> aiServiceClass) {
        super(message, cause);
        this.aiServiceClass = aiServiceClass;
    }

    public Class<?> getAiServiceClass() {
        return aiServiceClass;
    }
}
