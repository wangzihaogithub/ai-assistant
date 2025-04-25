package com.github.aiassistant.exception;

import com.github.aiassistant.entity.AiAssistantFewshot;

/**
 * Fewshot，小样本学习配置出现错误
 */
public class FewshotConfigException extends AiAssistantException {
    private final AiAssistantFewshot fewshot;

    public FewshotConfigException(String message, Throwable cause, AiAssistantFewshot fewshot) {
        super(message, cause);
        this.fewshot = fewshot;
    }

    public AiAssistantFewshot getFewshot() {
        return fewshot;
    }
}
