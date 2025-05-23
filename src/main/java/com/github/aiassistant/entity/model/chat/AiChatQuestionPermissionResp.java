package com.github.aiassistant.entity.model.chat;

// @Data
public class AiChatQuestionPermissionResp {
    /**
     * 今日字数
     */
    private Integer tokenCount;
    /**
     * 是否还有今日可用字数
     */
    private boolean hasTokens;
    /**
     * 针对该会话是否有权限
     */
    private boolean hasPermission;
    /**
     * 新的问题编号
     */
    private String userQueryTraceNumber;
    /**
     * 提问时间
     */
    private Long timestamp;

    public Integer getTokenCount() {
        return tokenCount;
    }

    public void setTokenCount(Integer tokenCount) {
        this.tokenCount = tokenCount;
    }

    public boolean isHasTokens() {
        return hasTokens;
    }

    public void setHasTokens(boolean hasTokens) {
        this.hasTokens = hasTokens;
    }

    public boolean isHasPermission() {
        return hasPermission;
    }

    public void setHasPermission(boolean hasPermission) {
        this.hasPermission = hasPermission;
    }

    public String getUserQueryTraceNumber() {
        return userQueryTraceNumber;
    }

    public void setUserQueryTraceNumber(String userQueryTraceNumber) {
        this.userQueryTraceNumber = userQueryTraceNumber;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }
}
