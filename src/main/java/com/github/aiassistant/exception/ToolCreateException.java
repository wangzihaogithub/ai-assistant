package com.github.aiassistant.exception;

import com.github.aiassistant.entity.AiTool;

/**
 * AI工具创建出现错误
 */
public class ToolCreateException extends AiAssistantException {
    private final AiTool aiTool;

    public ToolCreateException(String message, Throwable cause,
                               AiTool aiTool) {
        super(message, cause);
        this.aiTool = aiTool;
    }

    public AiTool getAiTool() {
        return aiTool;
    }
}
