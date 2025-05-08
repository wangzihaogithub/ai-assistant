package com.github.aiassistant.enums;

import java.util.Arrays;

public enum MessageTypeEnum {
    /**
     * @see dev.langchain4j.data.message.SystemMessage
     */
    System("System"),
    /**
     * @see dev.langchain4j.data.message.UserMessage
     */
    User("User"),
    /**
     * @see dev.langchain4j.data.message.ToolExecutionResultMessage
     */
    ToolResult("ToolResult"),
    /**
     * @see dev.langchain4j.data.message.AiMessage
     */
    Ai("Ai"),

    /**
     * @see com.github.aiassistant.entity.model.langchain4j.LangChainUserMessage
     */
    LangChainUser("LangChainUser"),
    /**
     * @see com.github.aiassistant.entity.model.langchain4j.MstateAiMessage
     */
    MState("MState"),
    /**
     * @see com.github.aiassistant.entity.model.langchain4j.KnowledgeAiMessage
     */
    Knowledge("Knowledge"),
    /**
     * @see com.github.aiassistant.entity.model.langchain4j.ThinkingAiMessage
     */
    Thinking("Thinking");

    private final String code;

    MessageTypeEnum(String code) {
        this.code = code;
    }

    public static MessageTypeEnum getByCode(String code) {
        return Arrays.stream(values())
                .filter(e -> e.code.equals(code))
                .findFirst()
                .orElse(null);
    }

    public static boolean isToolResult(MessageTypeEnum type) {
        return ToolResult == type;
    }

    public static boolean isChatType(MessageTypeEnum type) {
        return MessageTypeEnum.User == type
                || MessageTypeEnum.Ai == type
                || MessageTypeEnum.Thinking == type
                || MessageTypeEnum.ToolResult == type
                ;
    }

    public String getCode() {
        return code;
    }
}
