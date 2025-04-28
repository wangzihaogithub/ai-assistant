package com.github.aiassistant.entity;

import java.util.Date;

//// // @ApiModel(value = "AiMemoryMessageKn", description = "知识库查询结果")
//// @TableName("ai_memory_message_kn")
public class AiMemoryMessageKn {

    //     // @TableId(value = "id", type = IdType.AUTO)
    // // @ApiModelProperty(value = "ID", example = "1")
    private Integer id;
    // // @ApiModelProperty(value = "记忆消息ID", example = "1")
    private Integer aiMemoryMessageId;
    // // @ApiModelProperty(value = "知识库ID", example = "1")
    private Integer knId;
    // // @ApiModelProperty(value = "知识库问题文本（二进制）", example = "需要转换为可读的格式")
    private String knQuestionText;
    // // @ApiModelProperty(value = "知识库回答文本（整型，可能是一个ID或其他标识符）", example = "123")
    private String knAnswerText;
    // // @ApiModelProperty(value = "匹配度（百分之，乘以100后的）", example = "9500")
    private Long knScore;
    // // @ApiModelProperty(value = "知识库索引更新时间", example = "2023-01-01T00:00:00")
    private Date knIndexUpdatedTime;
    // // @ApiModelProperty(value = "知识库类型（ES索引名称）", example = "knowledge_index")
    private String knIndexName;
    // // @ApiModelProperty(value = "记忆ID", example = "1")
    private Integer aiMemoryId;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getAiMemoryMessageId() {
        return aiMemoryMessageId;
    }

    public void setAiMemoryMessageId(Integer aiMemoryMessageId) {
        this.aiMemoryMessageId = aiMemoryMessageId;
    }

    public Integer getKnId() {
        return knId;
    }

    public void setKnId(Integer knId) {
        this.knId = knId;
    }

    public String getKnQuestionText() {
        return knQuestionText;
    }

    public void setKnQuestionText(String knQuestionText) {
        this.knQuestionText = knQuestionText;
    }

    public String getKnAnswerText() {
        return knAnswerText;
    }

    public void setKnAnswerText(String knAnswerText) {
        this.knAnswerText = knAnswerText;
    }

    public Long getKnScore() {
        return knScore;
    }

    public void setKnScore(Long knScore) {
        this.knScore = knScore;
    }

    public Date getKnIndexUpdatedTime() {
        return knIndexUpdatedTime;
    }

    public void setKnIndexUpdatedTime(Date knIndexUpdatedTime) {
        this.knIndexUpdatedTime = knIndexUpdatedTime;
    }

    public String getKnIndexName() {
        return knIndexName;
    }

    public void setKnIndexName(String knIndexName) {
        this.knIndexName = knIndexName;
    }

    public Integer getAiMemoryId() {
        return aiMemoryId;
    }

    public void setAiMemoryId(Integer aiMemoryId) {
        this.aiMemoryId = aiMemoryId;
    }

    @Override
    public String toString() {
        return id + "#" + knAnswerText;
    }
}