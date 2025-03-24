package com.github.aiassistant.entity;

import java.util.Date;

// @Data
// @ApiModel(description = "向量缓存")
// @TableName("ai_embedding")
public class AiEmbedding {
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

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public String getVector() {
        return vector;
    }

    public void setVector(String vector) {
        this.vector = vector;
    }

    public String getMd5() {
        return md5;
    }

    public void setMd5(String md5) {
        this.md5 = md5;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public Integer getDimensions() {
        return dimensions;
    }

    public void setDimensions(Integer dimensions) {
        this.dimensions = dimensions;
    }

    // @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    private Date createTime;

    private String keyword;
    private String vector;
    private String md5;
    private String modelName;
    private Integer dimensions;

}