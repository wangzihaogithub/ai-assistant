package com.github.aiassistant.entity;

import java.io.Serializable;
import java.util.Date;

// @Data
// @ApiModel(value = "AiChatAbort", description = "用户点击终止生成")
// @TableName("ai_chat_abort")
public class AiChatAbort implements Serializable {
    // @TableId(value = "id", type = IdType.AUTO)
    // @ApiModelProperty(value = "ID", example = "1")
    private Integer id;
    // @ApiModelProperty(value = "创建时间", example = "2023-01-01T00:00:00")
    private Date createTime;
    // @ApiModelProperty(value = "终止前文本", example = "之前的文本内容")
    private String beforeText;
    // @ApiModelProperty(value = "聊天ID", example = "1")
    private Integer aiChatId;
    // @ApiModelProperty(value = "记忆ID", example = "1")
    private Integer aiMemoryId;
    // @ApiModelProperty(value = "用户问题聊天追踪号", example = "101")
    private String userQueryTraceNumber;
    // @ApiModelProperty(value = "用户问题聊天追踪号(原始问题编号)", example = "101")
    private String rootAgainUserQueryTraceNumber;
    // @ApiModelProperty(value = "第几个消息", example = "101")
    private Integer messageIndex;

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

    public String getBeforeText() {
        return beforeText;
    }

    public void setBeforeText(String beforeText) {
        this.beforeText = beforeText;
    }

    public Integer getAiChatId() {
        return aiChatId;
    }

    public void setAiChatId(Integer aiChatId) {
        this.aiChatId = aiChatId;
    }

    public Integer getAiMemoryId() {
        return aiMemoryId;
    }

    public void setAiMemoryId(Integer aiMemoryId) {
        this.aiMemoryId = aiMemoryId;
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

    public Integer getMessageIndex() {
        return messageIndex;
    }

    public void setMessageIndex(Integer messageIndex) {
        this.messageIndex = messageIndex;
    }

    @Override
    public String toString() {
        return id + "#" + beforeText;
    }
}