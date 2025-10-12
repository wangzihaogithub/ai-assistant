package com.github.aiassistant.entity;

import java.io.Serializable;
import java.util.Date;

// @Data
// @TableName("ai_variables")
public class AiVariables implements Serializable {
    // @TableId(value = "id", type = IdType.AUTO)
    private Integer id;
    private String varKey;
    private String varValue;
    private Date createTime;
    private Date updateTime;
    private Boolean enableFlag;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getVarKey() {
        return varKey;
    }

    public void setVarKey(String varKey) {
        this.varKey = varKey;
    }

    public String getVarValue() {
        return varValue;
    }

    public void setVarValue(String varValue) {
        this.varValue = varValue;
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

    public Boolean getEnableFlag() {
        return enableFlag;
    }

    public void setEnableFlag(Boolean enableFlag) {
        this.enableFlag = enableFlag;
    }

    @Override
    public String toString() {
        return id + "#" + varKey;
    }
}