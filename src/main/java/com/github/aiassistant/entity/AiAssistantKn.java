package com.github.aiassistant.entity;


import java.io.Serializable;

// @Data
// @TableName("ai_assistant_kn")
public class AiAssistantKn implements Serializable {
    // @TableId(value = "id", type = IdType.NONE)
    private Integer id;
    // @ApiModelProperty(value = "智能体ID", required = true)
    private String assistantId;
    // @ApiModelProperty(value = "搜索字段", required = true)
    private String vectorFieldName;
    // @ApiModelProperty(value = "最小匹配度", required = true)
    private Long minScore;
    // @ApiModelProperty(value = "最多召回几个", required = true)
    private Integer knLimit;
    // @ApiModelProperty(value = "知识库数据存储es", required = true)
    private String knIndexName;
    // @ApiModelProperty(value = "知识库类型枚举（qa=问答，majorjob=专业，job=岗位）", required = true)
    private String knTypeEnum;
    // @ApiModelProperty(value = "top1分数", required = true)
    private Long knTop1Score;
    // @ApiModelProperty(value = "knn搜索k的系数", required = true)
    private Integer knnFactor;
    // @ApiModelProperty(value = "知识库提问开启最小长度", required = true)
    private Integer knQueryMinCharLength;
    private String embeddingApiKey;
    private String embeddingBaseUrl;
    private String embeddingModelName;
    private Integer embeddingDimensions;
    private Integer embeddingMaxRequestSize;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getAssistantId() {
        return assistantId;
    }

    public void setAssistantId(String assistantId) {
        this.assistantId = assistantId;
    }

    public String getVectorFieldName() {
        return vectorFieldName;
    }

    public void setVectorFieldName(String vectorFieldName) {
        this.vectorFieldName = vectorFieldName;
    }

    public Long getMinScore() {
        return minScore;
    }

    public void setMinScore(Long minScore) {
        this.minScore = minScore;
    }

    public Integer getKnLimit() {
        return knLimit;
    }

    public void setKnLimit(Integer knLimit) {
        this.knLimit = knLimit;
    }

    public String getKnIndexName() {
        return knIndexName;
    }

    public void setKnIndexName(String knIndexName) {
        this.knIndexName = knIndexName;
    }

    public String getKnTypeEnum() {
        return knTypeEnum;
    }

    public void setKnTypeEnum(String knTypeEnum) {
        this.knTypeEnum = knTypeEnum;
    }

    public Long getKnTop1Score() {
        return knTop1Score;
    }

    public void setKnTop1Score(Long knTop1Score) {
        this.knTop1Score = knTop1Score;
    }

    public Integer getKnnFactor() {
        return knnFactor;
    }

    public void setKnnFactor(Integer knnFactor) {
        this.knnFactor = knnFactor;
    }

    public Integer getKnQueryMinCharLength() {
        return knQueryMinCharLength;
    }

    public void setKnQueryMinCharLength(Integer knQueryMinCharLength) {
        this.knQueryMinCharLength = knQueryMinCharLength;
    }

    public String getEmbeddingApiKey() {
        return embeddingApiKey;
    }

    public void setEmbeddingApiKey(String embeddingApiKey) {
        this.embeddingApiKey = embeddingApiKey;
    }

    public String getEmbeddingBaseUrl() {
        return embeddingBaseUrl;
    }

    public void setEmbeddingBaseUrl(String embeddingBaseUrl) {
        this.embeddingBaseUrl = embeddingBaseUrl;
    }

    public String getEmbeddingModelName() {
        return embeddingModelName;
    }

    public void setEmbeddingModelName(String embeddingModelName) {
        this.embeddingModelName = embeddingModelName;
    }

    public Integer getEmbeddingDimensions() {
        return embeddingDimensions;
    }

    public void setEmbeddingDimensions(Integer embeddingDimensions) {
        this.embeddingDimensions = embeddingDimensions;
    }

    public Integer getEmbeddingMaxRequestSize() {
        return embeddingMaxRequestSize;
    }

    public void setEmbeddingMaxRequestSize(Integer embeddingMaxRequestSize) {
        this.embeddingMaxRequestSize = embeddingMaxRequestSize;
    }

    @Override
    public String toString() {
        return id + "#" + assistantId + "." + knIndexName;
    }
}