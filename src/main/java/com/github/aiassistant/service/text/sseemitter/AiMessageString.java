package com.github.aiassistant.service.text.sseemitter;

import java.util.Map;

/**
 * ai消息字符串（区分聊天和记忆）
 * 例如：聊天记录需要展示aaa，记忆需要存储bbb
 */
public class AiMessageString {
    /**
     * 聊天记录内容
     */
    private final String chatString;
    /**
     * 记忆内容
     */
    private final String memoryString;
    /**
     * 回复标识
     */
    private final Map<String, Object> stringMetaMap;

    public AiMessageString(String chatString, String memoryString, Map<String, Object> stringMetaMap) {
        this.chatString = chatString;
        this.memoryString = memoryString;
        this.stringMetaMap = stringMetaMap;
    }

    public AiMessageString(String chatString, String memoryString) {
        this.chatString = chatString;
        this.memoryString = memoryString;
        this.stringMetaMap = null;
    }

    public AiMessageString(String string, Map<String, Object> stringMetaMap) {
        this.memoryString = string;
        this.chatString = string;
        this.stringMetaMap = stringMetaMap;
    }

    public AiMessageString(String string) {
        this.memoryString = string;
        this.chatString = string;
        this.stringMetaMap = null;
    }

    public Map<String, Object> getStringMetaMap() {
        return stringMetaMap;
    }

    public String getMemoryString() {
        return memoryString;
    }

    public String getChatString() {
        return chatString;
    }

    public boolean isEmpty() {
        return (memoryString == null || memoryString.isEmpty())
                && (chatString == null || chatString.isEmpty());
    }

    @Override
    public String toString() {
        return chatString;
    }
}
