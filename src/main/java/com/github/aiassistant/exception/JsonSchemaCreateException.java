package com.github.aiassistant.exception;

import com.github.aiassistant.entity.AiJsonschema;

/**
 * 创建Jsonschema出现错误
 */
public class JsonSchemaCreateException extends AiAssistantException {
    private final AiJsonschema aiJsonschema;

    public JsonSchemaCreateException(String message, Throwable cause,
                                     AiJsonschema aiJsonschema) {
        super(message, cause);
        this.aiJsonschema = aiJsonschema;
    }

    public AiJsonschema getAiJsonschema() {
        return aiJsonschema;
    }
}
