package com.github.aiassistant.exception;

/**
 * 供应商的模型API生成失败
 */
public class ModelApiGenerateException extends AiAssistantException {
    private final String modelName;

    public ModelApiGenerateException(String message, Throwable cause,
                                     String modelName) {
        super(message, cause);
        this.modelName = modelName;
    }

    public String getModelName() {
        return modelName;
    }
}
