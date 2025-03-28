package com.github.aiassistant.enums;

import java.util.Arrays;

/**
 * ai_chat表的数据库时间字段，用于接口调用查询排序
 */
public enum AiChatTimeEnum {
    createTime("create_time"),
    updateTime("update_Time"),
    lastChatTime("last_Chat_Time");

    private final String columnName;

    AiChatTimeEnum(String columnName) {
        this.columnName = columnName;
    }

    public static AiChatTimeEnum getByColumnName(String columnName) {
        return Arrays.stream(values())
                .filter(e -> e.columnName.equalsIgnoreCase(columnName))
                .findFirst()
                .orElse(null);
    }

    public String getColumnName() {
        return columnName;
    }
}
