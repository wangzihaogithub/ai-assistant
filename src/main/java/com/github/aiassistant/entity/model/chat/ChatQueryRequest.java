package com.github.aiassistant.entity.model.chat;

import com.github.aiassistant.util.UniqueKeyGenerator;

// @Data
public class ChatQueryRequest {
    private String question;
    private Integer chatId;
    // @ApiModelProperty("重新回答的序列号")
    private String againUserQueryTraceNumber;
    // @ApiModelProperty("提问的序列号，前端掉后端接口生成，用于后端还没返回ID时，用户点击终止")
    private String userQueryTraceNumber;
    // @ApiModelProperty("联网")
    private Boolean websearch = true;
    // @ApiModelProperty("思考")
    private Boolean reasoning = true;
    private Long timestamp;

    public static String newUserQueryTraceNumber() {
        return UniqueKeyGenerator.nextId();
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    @Deprecated
    public void setQuery(String query) {
        this.question = query;
    }

    public Integer getChatId() {
        return chatId;
    }

    public void setChatId(Integer chatId) {
        this.chatId = chatId;
    }

    public String getAgainUserQueryTraceNumber() {
        return againUserQueryTraceNumber;
    }

    public void setAgainUserQueryTraceNumber(String againUserQueryTraceNumber) {
        this.againUserQueryTraceNumber = againUserQueryTraceNumber;
    }

    public String getUserQueryTraceNumber() {
        return userQueryTraceNumber;
    }

    public void setUserQueryTraceNumber(String userQueryTraceNumber) {
        this.userQueryTraceNumber = userQueryTraceNumber;
    }

    public Boolean getWebsearch() {
        return websearch;
    }

    public void setWebsearch(Boolean websearch) {
        this.websearch = websearch;
    }

    public Boolean getReasoning() {
        return reasoning;
    }

    public void setReasoning(Boolean reasoning) {
        this.reasoning = reasoning;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public String userQueryTraceNumber() {
        if (userQueryTraceNumber == null || userQueryTraceNumber.isEmpty()) {
            userQueryTraceNumber = newUserQueryTraceNumber();
        }
        return userQueryTraceNumber;
    }

    public long timestamp() {
        if (timestamp == null) {
            timestamp = System.currentTimeMillis();
        }
        return timestamp;
    }
}
