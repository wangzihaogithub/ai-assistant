package com.github.aiassistant.entity.model.chat;

// @Data
public class AiChatTokenVO {
    private Integer tokenCount;

    /**
     * 是否还有今日可用字数
     * @param maxTokenCount maxTokenCount
     * @return 是否还有今日可用字数
     */
    public boolean isHasTokens(int maxTokenCount) {
        return tokenCount < maxTokenCount;
    }

    public Integer getTokenCount() {
        return tokenCount;
    }

    public void setTokenCount(Integer tokenCount) {
        this.tokenCount = tokenCount;
    }
}
