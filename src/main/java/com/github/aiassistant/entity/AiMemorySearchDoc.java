package com.github.aiassistant.entity;

import java.io.Serializable;

// @ApiModel(value = "AiMemorySearchDoc", description = "记忆的搜索记录文档")
// @Data
// @TableName("ai_memory_search_doc")
public class AiMemorySearchDoc implements Serializable {
    // @TableId(value = "id", type = IdType.AUTO)
    private Integer id;
    /**
     * 记忆ID
     */
    private Integer aiMemorySearchId;
    /**
     * 查询索引对象
     */
    private String docIdString;
    /**
     * 聊天ID
     */
    private Integer docIdInt;
    /**
     * 查询条件
     */
    private Integer docScore;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getAiMemorySearchId() {
        return aiMemorySearchId;
    }

    public void setAiMemorySearchId(Integer aiMemorySearchId) {
        this.aiMemorySearchId = aiMemorySearchId;
    }

    public String getDocIdString() {
        return docIdString;
    }

    public void setDocIdString(String docIdString) {
        this.docIdString = docIdString;
    }

    public Integer getDocIdInt() {
        return docIdInt;
    }

    public void setDocIdInt(Integer docIdInt) {
        this.docIdInt = docIdInt;
    }

    public Integer getDocScore() {
        return docScore;
    }

    public void setDocScore(Integer docScore) {
        this.docScore = docScore;
    }

    @Override
    public String toString() {
        return id + "#" + aiMemorySearchId + "(" + docIdString + ")";
    }
}