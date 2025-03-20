package com.github.aiassistant.entity;

// import com.baomidou.mybatisplus.annotation.IdType;
// import com.baomidou.mybatisplus.annotation.TableId;
// import com.baomidou.mybatisplus.annotation.TableName;
// import io.swagger.annotations.ApiModel;
// import io.swagger.annotations.ApiModelProperty;
// import lombok.Data;

import java.util.Date;

// @ApiModel(value = "AiMemoryMessage", description = "记忆的消息")
// @Data
// @TableName("ai_memory_message")
public class AiMemoryMessage {
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getAiMemoryId() {
        return aiMemoryId;
    }

    public void setAiMemoryId(Integer aiMemoryId) {
        this.aiMemoryId = aiMemoryId;
    }

    public Integer getMessageIndex() {
        return messageIndex;
    }

    public void setMessageIndex(Integer messageIndex) {
        this.messageIndex = messageIndex;
    }

    public String getMessageText() {
        return messageText;
    }

    public void setMessageText(String messageText) {
        this.messageText = messageText;
    }

    public String getMessageTypeEnum() {
        return messageTypeEnum;
    }

    public void setMessageTypeEnum(String messageTypeEnum) {
        this.messageTypeEnum = messageTypeEnum;
    }

    public Boolean getUserQueryFlag() {
        return userQueryFlag;
    }

    public void setUserQueryFlag(Boolean userQueryFlag) {
        this.userQueryFlag = userQueryFlag;
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

    public Date getCommitTime() {
        return commitTime;
    }

    public void setCommitTime(Date commitTime) {
        this.commitTime = commitTime;
    }

    public Date getFirstTokenTime() {
        return firstTokenTime;
    }

    public void setFirstTokenTime(Date firstTokenTime) {
        this.firstTokenTime = firstTokenTime;
    }

    public Boolean getUseKnowledgeFlag() {
        return useKnowledgeFlag;
    }

    public void setUseKnowledgeFlag(Boolean useKnowledgeFlag) {
        this.useKnowledgeFlag = useKnowledgeFlag;
    }

    public Boolean getUseToolFlag() {
        return useToolFlag;
    }

    public void setUseToolFlag(Boolean useToolFlag) {
        this.useToolFlag = useToolFlag;
    }

    public String getReplyToolRequestId() {
        return replyToolRequestId;
    }

    public void setReplyToolRequestId(String replyToolRequestId) {
        this.replyToolRequestId = replyToolRequestId;
    }

    public String getReplyToolName() {
        return replyToolName;
    }

    public void setReplyToolName(String replyToolName) {
        this.replyToolName = replyToolName;
    }

    public Integer getTokenCount() {
        return tokenCount;
    }

    public void setTokenCount(Integer tokenCount) {
        this.tokenCount = tokenCount;
    }

    public Integer getCharLength() {
        return charLength;
    }

    public void setCharLength(Integer charLength) {
        this.charLength = charLength;
    }

    public Integer getUserTokenCount() {
        return userTokenCount;
    }

    public void setUserTokenCount(Integer userTokenCount) {
        this.userTokenCount = userTokenCount;
    }

    public Integer getAiTokenCount() {
        return aiTokenCount;
    }

    public void setAiTokenCount(Integer aiTokenCount) {
        this.aiTokenCount = aiTokenCount;
    }

    public Integer getKnowledgeTokenCount() {
        return knowledgeTokenCount;
    }

    public void setKnowledgeTokenCount(Integer knowledgeTokenCount) {
        this.knowledgeTokenCount = knowledgeTokenCount;
    }

    public Integer getUserCharLength() {
        return userCharLength;
    }

    public void setUserCharLength(Integer userCharLength) {
        this.userCharLength = userCharLength;
    }

    public Integer getKnowledgeCharLength() {
        return knowledgeCharLength;
    }

    public void setKnowledgeCharLength(Integer knowledgeCharLength) {
        this.knowledgeCharLength = knowledgeCharLength;
    }

    public Integer getAiCharLength() {
        return aiCharLength;
    }

    public void setAiCharLength(Integer aiCharLength) {
        this.aiCharLength = aiCharLength;
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

    public Date getDeleteTime() {
        return deleteTime;
    }

    public void setDeleteTime(Date deleteTime) {
        this.deleteTime = deleteTime;
    }

    public String getOpenAiRequestId() {
        return openAiRequestId;
    }

    public void setOpenAiRequestId(String openAiRequestId) {
        this.openAiRequestId = openAiRequestId;
    }

    public Boolean getWebsearchFlag() {
        return websearchFlag;
    }

    public void setWebsearchFlag(Boolean websearchFlag) {
        this.websearchFlag = websearchFlag;
    }

    // @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    private Integer aiMemoryId;

    private Integer messageIndex;

    private String messageText;

    private String messageTypeEnum;

    private Boolean userQueryFlag; // bit(1) 类型在 Java 中通常映射为 boolean

    private Date createTime;
    /**
     * 开始时间
     */
    private Date startTime;
    private Date commitTime;
    private Date firstTokenTime;
    private Boolean useKnowledgeFlag; // bit(1) 类型在 Java 中通常映射为 boolean

    private Boolean useToolFlag; // bit(1) 类型在 Java 中通常映射为 boolean

    private String replyToolRequestId;

    private String replyToolName;

    private Integer tokenCount;
    private Integer charLength;

    private Integer userTokenCount;
    private Integer aiTokenCount;
    private Integer knowledgeTokenCount;

    private Integer userCharLength;
    private Integer knowledgeCharLength;
    private Integer aiCharLength;

    // @ApiModelProperty(value = "用户问题聊天追踪号", example = "101")
    private String userQueryTraceNumber;

    // @ApiModelProperty(value = "重新回答用户问题聊天追踪号", example = "101")
    private String againUserQueryTraceNumber;

    // @ApiModelProperty(value = "重新回答用户问题聊天追踪号，根问题", example = "101")
    private String rootUserQueryTraceNumber;

    // @ApiModelProperty(value = "数据是哪个阶段生产出来的，取值范围【Request,Response】", example = "101")
    private String stageEnum;

    private Date deleteTime;

    // @ApiModelProperty(value = "供应商的请求id，用于向供应商反馈错误问题", example = "101")
    private String openAiRequestId;

    // @ApiModelProperty(value = "是否联网", example = "101")
    private Boolean websearchFlag;
}