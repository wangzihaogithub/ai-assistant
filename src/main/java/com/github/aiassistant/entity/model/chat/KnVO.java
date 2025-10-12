package com.github.aiassistant.entity.model.chat;

import com.github.aiassistant.util.AiUtil;

import java.io.Serializable;
import java.util.Date;

/**
 * 知识库
 */
public class KnVO implements Serializable {
    private String id;
    private Double score;
    private String indexName;
    private Date indexUpdatedTime;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
    }

    public Long scoreLong() {
        return AiUtil.scoreToLong(score);
    }

    public String getIndexName() {
        return indexName;
    }

    public void setIndexName(String indexName) {
        this.indexName = indexName;
    }

    public Date getIndexUpdatedTime() {
        return indexUpdatedTime;
    }

    public void setIndexUpdatedTime(Date indexUpdatedTime) {
        this.indexUpdatedTime = indexUpdatedTime;
    }
}
