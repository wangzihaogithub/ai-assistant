package com.github.aiassistant.entity;

import java.util.Date;

// @Data
// @ApiModel(description = "记忆")
// @TableName("ai_memory")
public class AiMemory {
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

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
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

    // @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    private Date createTime;

    private Date updateTime;

    private Integer userTokenCount;
    private Integer aiTokenCount;
    private Integer knowledgeTokenCount;

    private Integer userCharLength;
    private Integer knowledgeCharLength;
    private Integer aiCharLength;
}