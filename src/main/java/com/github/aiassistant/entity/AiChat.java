package com.github.aiassistant.entity;

import java.util.Date;

//// @Data
//// @TableName("ai_chat")
public class AiChat {
//    // @TableId(value = "id", type = IdType.AUTO)
    private Integer id;
    private String name;
    private Date createTime;
    private Date updateTime;
    private Date lastChatTime;
    private Boolean lastWebsearchFlag;
    private Integer createUid;
    private Integer aiMemoryId;
    private Date deleteTime;
    private String assistantId;
    private String uidType; // 用户类型，类型等于表名，student=学生，sys_user=员工

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public Date getLastChatTime() {
        return lastChatTime;
    }

    public void setLastChatTime(Date lastChatTime) {
        this.lastChatTime = lastChatTime;
    }

    public Boolean getLastWebsearchFlag() {
        return lastWebsearchFlag;
    }

    public void setLastWebsearchFlag(Boolean lastWebsearchFlag) {
        this.lastWebsearchFlag = lastWebsearchFlag;
    }

    public Integer getCreateUid() {
        return createUid;
    }

    public void setCreateUid(Integer createUid) {
        this.createUid = createUid;
    }

    public Integer getAiMemoryId() {
        return aiMemoryId;
    }

    public void setAiMemoryId(Integer aiMemoryId) {
        this.aiMemoryId = aiMemoryId;
    }

    public Date getDeleteTime() {
        return deleteTime;
    }

    public void setDeleteTime(Date deleteTime) {
        this.deleteTime = deleteTime;
    }

    public String getAssistantId() {
        return assistantId;
    }

    public void setAssistantId(String assistantId) {
        this.assistantId = assistantId;
    }

    public String getUidType() {
        return uidType;
    }

    public void setUidType(String uidType) {
        this.uidType = uidType;
    }
}