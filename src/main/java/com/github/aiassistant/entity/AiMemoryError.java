package com.github.aiassistant.entity;

import java.util.Date;

// @Data
// @TableName("ai_memory_error")
public class AiMemoryError {
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getAiChatId() {
        return aiChatId;
    }

    public void setAiChatId(Integer aiChatId) {
        this.aiChatId = aiChatId;
    }

    public Integer getMemoryId() {
        return memoryId;
    }

    public void setMemoryId(Integer memoryId) {
        this.memoryId = memoryId;
    }

    public String getUserQueryTraceNumber() {
        return userQueryTraceNumber;
    }

    public void setUserQueryTraceNumber(String userQueryTraceNumber) {
        this.userQueryTraceNumber = userQueryTraceNumber;
    }

    public String getRootAgainUserQueryTraceNumber() {
        return rootAgainUserQueryTraceNumber;
    }

    public void setRootAgainUserQueryTraceNumber(String rootAgainUserQueryTraceNumber) {
        this.rootAgainUserQueryTraceNumber = rootAgainUserQueryTraceNumber;
    }

    public Integer getMessageCount() {
        return messageCount;
    }

    public void setMessageCount(Integer messageCount) {
        this.messageCount = messageCount;
    }

    public String getErrorClassName() {
        return errorClassName;
    }

    public void setErrorClassName(String errorClassName) {
        this.errorClassName = errorClassName;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Integer getBaseMessageIndex() {
        return baseMessageIndex;
    }

    public void setBaseMessageIndex(Integer baseMessageIndex) {
        this.baseMessageIndex = baseMessageIndex;
    }

    public Integer getAddMessageCount() {
        return addMessageCount;
    }

    public void setAddMessageCount(Integer addMessageCount) {
        this.addMessageCount = addMessageCount;
    }

    public Integer getGenerateCount() {
        return generateCount;
    }

    public void setGenerateCount(Integer generateCount) {
        this.generateCount = generateCount;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getSessionTime() {
        return sessionTime;
    }

    public void setSessionTime(Date sessionTime) {
        this.sessionTime = sessionTime;
    }

    public String getErrorType() {
        return errorType;
    }

    public void setErrorType(String errorType) {
        this.errorType = errorType;
    }

    public String getMessageText() {
        return messageText;
    }

    public void setMessageText(String messageText) {
        this.messageText = messageText;
    }

    public String getAttachmentJson() {
        return attachmentJson;
    }

    public void setAttachmentJson(String attachmentJson) {
        this.attachmentJson = attachmentJson;
    }

    // @TableId(value = "id", type = IdType.AUTO)
    private Integer id;
    private Integer aiChatId;
    private Integer memoryId;
    
    private String userQueryTraceNumber;
    private String rootAgainUserQueryTraceNumber;
    private Integer messageCount;

    private String errorClassName;
    private String errorMessage;
    private Integer baseMessageIndex;
    private Integer addMessageCount;
    private Integer generateCount;

    private Date createTime;
    private Date sessionTime;

    private String errorType;
    private String messageText;
    private String attachmentJson;

    @Override
    public String toString() {
        return id + "#" + errorClassName;
    }
}