package com.github.aiassistant.enums;

import java.util.Arrays;

public enum MessageTypeEnum {
    User("User"),
    System("System"),
    OutofScope("OutofScope"),
    LangChainUser("LangChainUser"),
    ToolResult("ToolResult"),
    Ai("Ai"),
    MState("MState"),
    Knowledge("Knowledge"),
    Thinking("Thinking");

    private final String code;

    MessageTypeEnum(String code) {
        this.code = code;
    }

    public static MessageTypeEnum getByCode(String code) {
        return Arrays.stream(values())
                .filter(e -> e.getCode().equals(code))
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
