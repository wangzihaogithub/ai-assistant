package com.github.aiassistant.exception;

/**
 * json模型返回结果解析出现错误
 */
public class JsonschemaResultParseException extends AiAssistantException {
    private final Class<?> aiServiceClass;

    public JsonschemaResultParseException(String message, Throwable cause, Class<?> aiServiceClass) {
        super(message, cause);
        this.aiServiceClass = aiServiceClass;
    }

    public Class<?> getAiServiceClass() {
        return aiServiceClass;
    }
}
