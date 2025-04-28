package com.github.aiassistant.entity;

import java.util.Date;

// @Data
// @ApiModel(value = "AiChatHistory", description = "聊天记录")
// @TableName("ai_chat_history")
public class AiChatHistory {
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

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getDeleteTime() {
        return deleteTime;
    }

    public void setDeleteTime(Date deleteTime) {
        this.deleteTime = deleteTime;
    }

    public String getMessageTypeEnum() {
        return messageTypeEnum;
    }

    public void setMessageTypeEnum(String messageTypeEnum) {
        this.messageTypeEnum = messageTypeEnum;
    }

    public String getMessageText() {
        return messageText;
    }

    public void setMessageText(String messageText) {
        this.messageText = messageText;
    }

    public Integer getTextCharLength() {
        return textCharLength;
    }

    public void setTextCharLength(Integer textCharLength) {
        this.textCharLength = textCharLength;
    }

    public Integer getMessageIndex() {
        return messageIndex;
    }

    public void setMessageIndex(Integer messageIndex) {
        this.messageIndex = messageIndex;
    }

    public Integer getUserChatHistoryId() {
        return userChatHistoryId;
    }

    public void setUserChatHistoryId(Integer userChatHistoryId) {
        this.userChatHistoryId = userChatHistoryId;
    }

    public String getUserQueryTraceNumber() {
        return userQueryTraceNumber;
    }

    public void setUserQueryTraceNumber(String userQueryTraceNumber) {
        this.userQueryTraceNumber = userQueryTraceNumber;
    }

    public String getAgainUserQueryTraceNumber() {
        return againUserQueryTraceNumber;
    }

    public void setAgainUserQueryTraceNumber(String againUserQueryTraceNumber) {
        this.againUserQueryTraceNumber = againUserQueryTraceNumber;
    }

    public String getRootUserQueryTraceNumber() {
        return rootUserQueryTraceNumber;
    }

    public void setRootUserQueryTraceNumber(String rootUserQueryTraceNumber) {
        this.rootUserQueryTraceNumber = rootUserQueryTraceNumber;
    }

    public String getStageEnum() {
        return stageEnum;
    }

    public void setStageEnum(String stageEnum) {
        this.stageEnum = stageEnum;
    }

    public Boolean getWebsearchFlag() {
        return websearchFlag;
    }

    public void setWebsearchFlag(Boolean websearchFlag) {
        this.websearchFlag = websearchFlag;
    }

    public Boolean getUserQueryFlag() {
        return userQueryFlag;
    }

    public void setUserQueryFlag(Boolean userQueryFlag) {
        this.userQueryFlag = userQueryFlag;
    }

    // @TableId(value = "id", type = IdType.AUTO)
    // @ApiModelProperty(value = "ID", example = "1")
    private Integer id;

    // @ApiModelProperty(value = "AI聊天ID", example = "101")
    private Integer aiChatId;

    // @ApiModelProperty(value = "创建时间", example = "2023-04-01T12:00:00")
    private Date createTime;
    /**
     * 开始时间
     */
    private Date startTime;
    // @ApiModelProperty(value = "删除时间，重新回答会删除", example = "2023-04-01T12:00:00")
    private Date deleteTime;

    // @ApiModelProperty(value = "消息类型枚举", example = "User", notes = "消息类型 User(\"User\"),\n" +
//            "    System(\"System\"),\n" +
//            "    ToolResult(\"ToolResult\"),\n" +
//            "    Ai(\"Ai\");")
    private String messageTypeEnum;

    // @ApiModelProperty(value = "文本消息", example = "Hello, how are you?")
    private String messageText;

    private Integer textCharLength;

    // @ApiModelProperty(value = "消息索引", example = "1")
    private Integer messageIndex;

    // @ApiModelProperty(value = "用户问题聊天ID", example = "101")
    private Integer userChatHistoryId;

    // @ApiModelProperty(value = "用户问题聊天追踪号", example = "101")
    private String userQueryTraceNumber;

    // @ApiModelProperty(value = "重新回答用户问题聊天追踪号", example = "101")
    private String againUserQueryTraceNumber;

    // @ApiModelProperty(value = "重新回答用户问题聊天追踪号，根问题", example = "101")
    private String rootUserQueryTraceNumber;

    // @ApiModelProperty(value = "数据是哪个阶段生产出来的，取值范围【Request,Response】", example = "101")
    private String stageEnum;
    // @ApiModelProperty(value = "是否联网", example = "101")
    private Boolean websearchFlag;
    private Boolean userQueryFlag; // bit(1) 类型在 Java 中通常映射为 boolean

    @Override
    public String toString() {
        return id + "#" + messageIndex;
    }
}