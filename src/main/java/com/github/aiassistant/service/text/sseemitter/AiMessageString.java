package com.github.aiassistant.service.text.sseemitter;

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

    public AiMessageString(String chatString, String memoryString) {
        this.chatString = chatString;
        this.memoryString = memoryString;
    }

    public AiMessageString(String string) {
        this.memoryString = string;
        this.chatString = string;
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
