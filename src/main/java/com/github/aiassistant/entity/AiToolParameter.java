package com.github.aiassistant.entity;

import java.io.Serializable;

/**
 * AI工具参数提示词
 * UNIQUE INDEX `uniq_tool_parameter_enum`(`ai_tool_id`, `parameter_enum`) USING BTREE
 */
// @Data
// @TableName("ai_tool_parameter")
public class AiToolParameter implements Serializable {
    // @TableId(value = "id", type = IdType.AUTO)
    private Integer id;
    private Integer aiToolId;
    /**
     * 参数枚举（就是研发实现的工具类对象的方法名）
     * UNIQUE INDEX `uniq_tool_parameter_enum`(`ai_tool_id`, `parameter_enum`) USING BTREE
     */
    private String parameterEnum;
    /**
     * 参数提示词（给AI用的）
     */
    private String parameterDescription;
    /**
     * 默认值
     */
    private String defaultValue;
    /**
     * 是否必填
     */
    private Boolean requiredFlag;
    /**
     * 是否开启这个参数
     */
    private Boolean enableFlag;
    /**
     * 是否给AI使用
     */
    private Boolean aiUseFlag;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getAiToolId() {
        return aiToolId;
    }

    public void setAiToolId(Integer aiToolId) {
        this.aiToolId = aiToolId;
    }

    public String getParameterEnum() {
        return parameterEnum;
    }

    public void setParameterEnum(String parameterEnum) {
        this.parameterEnum = parameterEnum;
    }

    public String getParameterDescription() {
        return parameterDescription;
    }

    public void setParameterDescription(String parameterDescription) {
        this.parameterDescription = parameterDescription;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public Boolean getRequiredFlag() {
        return requiredFlag;
    }

    public void setRequiredFlag(Boolean requiredFlag) {
        this.requiredFlag = requiredFlag;
    }

    public Boolean getEnableFlag() {
        return enableFlag;
    }

    public void setEnableFlag(Boolean enableFlag) {
        this.enableFlag = enableFlag;
    }

    public Boolean getAiUseFlag() {
        return aiUseFlag;
    }

    public void setAiUseFlag(Boolean aiUseFlag) {
        this.aiUseFlag = aiUseFlag;
    }

    @Override
    public String toString() {
        return id + "#" + parameterEnum;
    }
}