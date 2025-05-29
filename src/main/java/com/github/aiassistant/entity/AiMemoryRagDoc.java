package com.github.aiassistant.entity;

// @ApiModel(value = "AiMemoryRagDoc", description = "记忆的RAG记录文档")
// @Data
// @TableName("ai_memory_rag_doc")
public class AiMemoryRagDoc {
    // @TableId(value = "id", type = IdType.AUTO)
    private Integer id;
    /**
     * 记忆ID
     */
    private Integer aiMemoryRagId;
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

    public Integer getAiMemoryRagId() {
        return aiMemoryRagId;
    }

    public void setAiMemoryRagId(Integer aiMemoryRagId) {
        this.aiMemoryRagId = aiMemoryRagId;
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
        return id + "#" + aiMemoryRagId + "(" + docIdString + ")";
    }
}