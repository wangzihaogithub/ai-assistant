package com.github.aiassistant.entity.model.chat;

import java.util.Date;

// @Data
public class AiChatHistoryResp {
    // @ApiModelProperty(value = "ID", example = "1")
    private Integer id;
    // @ApiModelProperty(value = "创建时间", example = "2023-04-01T12:00:00")
    private Date createTime;
    // @ApiModelProperty(value = "删除时间", example = "2023-04-01T12:00:00")
    private Date deleteTime;
    /**
     * 开始时间
     */
    private Date startTime;
    // @ApiModelProperty(value = "消息类型枚举", example = "User", notes = "消息类型 User(\"User\"),\n" +
//            "    System(\"System\"),\n" +
//            "    ToolResult(\"ToolResult\"),\n" +
//            "    Ai(\"Ai\");")
    private String messageTypeEnum;
    // @ApiModelProperty(value = "文本消息", example = "Hello, how are you?")
    private String messageText;
    // @ApiModelProperty(value = "消息索引", example = "1")
    private Integer messageIndex;
    // @ApiModelProperty(value = "用户问题聊天ID", example = "101")
    private Integer userChatHistoryId;
    // @ApiModelProperty(value = "用户问题聊天追踪号", example = "101")
    private String userQueryTraceNumber;
    private Boolean userQueryFlag; // bit(1) 类型在 Java 中通常映射为 boolean
    private Boolean websearchFlag;
    /**
     * 是否思考
     */
    private Boolean reasoningFlag;
    // @ApiModelProperty(value = "重新回答用户问题聊天追踪号", example = "101")
    private String againUserQueryTraceNumber;

    //     private List<KnEsJob<AiChatHistoryJobResp>> jobList;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getDeleteTime() {
        return deleteTime;
    }

    public void setDeleteTime(Date deleteTime) {
        this.deleteTime = deleteTime;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
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

//    public List<KnEsJob<AiChatHistoryJobResp>> getJobList() {
//        return jobList;
//    }
//
//    public void setJobList(List<KnEsJob<AiChatHistoryJobResp>> jobList) {
//        this.jobList = jobList;
//    }

    public void setMessageText(String messageText) {
        this.messageText = messageText;
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

    public Boolean getUserQueryFlag() {
        return userQueryFlag;
    }

    public void setUserQueryFlag(Boolean userQueryFlag) {
        this.userQueryFlag = userQueryFlag;
    }

    public Boolean getWebsearchFlag() {
        return websearchFlag;
    }

    public void setWebsearchFlag(Boolean websearchFlag) {
        this.websearchFlag = websearchFlag;
    }

    public Boolean getReasoningFlag() {
        return reasoningFlag;
    }

    public void setReasoningFlag(Boolean reasoningFlag) {
        this.reasoningFlag = reasoningFlag;
    }

    public String getAgainUserQueryTraceNumber() {
        return againUserQueryTraceNumber;
    }

//    private List<KnEsJob<AiChatHistoryJobResp>> jobList;

    public void setAgainUserQueryTraceNumber(String againUserQueryTraceNumber) {
        this.againUserQueryTraceNumber = againUserQueryTraceNumber;
    }

    @Override
    public String toString() {
        return id + "#" + messageText;
    }
}
