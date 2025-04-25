package com.github.aiassistant.exception;

/**
 * AI异常
 */
public class AiAssistantException extends Exception {
    public AiAssistantException(String message) {
        super(message);
    }

    public AiAssistantException(String message, Throwable cause) {
        super(message, cause);
    }
}
