package com.github.aiassistant.entity;

import java.util.Date;

//// @ApiModel(value = "AiMemoryMstate", description = "状态记忆数据\n")
//// @Data
//// @TableName("ai_memory_mstate")
public class AiMemoryMstate {

//    // @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    private Integer aiMemoryId;

    private String stateKey;
    private String stateValue;
    private Integer userAiMemoryMessageId;
    private Integer userMessageIndex;
    private Date createTime;
    private Boolean knownFlag;
    private String userQueryTraceNumber;

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

    public String getStateKey() {
        return stateKey;
    }

    public void setStateKey(String stateKey) {
        this.stateKey = stateKey;
    }

    public String getStateValue() {
        return stateValue;
    }

    public void setStateValue(String stateValue) {
        this.stateValue = stateValue;
    }

    public Integer getUserAiMemoryMessageId() {
        return userAiMemoryMessageId;
    }

    public void setUserAiMemoryMessageId(Integer userAiMemoryMessageId) {
        this.userAiMemoryMessageId = userAiMemoryMessageId;
    }

    public Integer getUserMessageIndex() {
        return userMessageIndex;
    }

    public void setUserMessageIndex(Integer userMessageIndex) {
        this.userMessageIndex = userMessageIndex;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Boolean getKnownFlag() {
        return knownFlag;
    }

    public void setKnownFlag(Boolean knownFlag) {
        this.knownFlag = knownFlag;
    }

    public String getUserQueryTraceNumber() {
        return userQueryTraceNumber;
    }

    public void setUserQueryTraceNumber(String userQueryTraceNumber) {
        this.userQueryTraceNumber = userQueryTraceNumber;
    }

    @Override
    public String toString() {
        return id + "#" + stateKey;
    }
}