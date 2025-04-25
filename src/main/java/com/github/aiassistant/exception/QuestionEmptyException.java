package com.github.aiassistant.exception;

/**
 * 提问不能为空
 */
public class QuestionEmptyException extends AiAssistantException {

    public QuestionEmptyException(String message) {
        super(message);
    }
}
