package com.github.aiassistant.entity;

import java.util.Date;

/**
 * AI工具提示词
 * UNIQUE INDEX `uniq_tool_function_name`(`tool_function_name`) USING BTREE,
 * UNIQUE INDEX `uniq_tool_enum_function_enum`(`tool_enum`, `tool_function_enum`) USING BTREE
 */
// @Data
// @TableName("ai_tool")
public class AiTool {
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getToolFunctionName() {
        return toolFunctionName;
    }

    public void setToolFunctionName(String toolFunctionName) {
        this.toolFunctionName = toolFunctionName;
    }

    public String getToolFunctionEnum() {
        return toolFunctionEnum;
    }

    public void setToolFunctionEnum(String toolFunctionEnum) {
        this.toolFunctionEnum = toolFunctionEnum;
    }

    public String getToolEnum() {
        return toolEnum;
    }

    public void setToolEnum(String toolEnum) {
        this.toolEnum = toolEnum;
    }

    public String getToolFunctionDescription() {
        return toolFunctionDescription;
    }

    public void setToolFunctionDescription(String toolFunctionDescription) {
        this.toolFunctionDescription = toolFunctionDescription;
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

    public Integer getCreateUid() {
        return createUid;
    }

    public void setCreateUid(Integer createUid) {
        this.createUid = createUid;
    }

    public Integer getUpdateUid() {
        return updateUid;
    }

    public void setUpdateUid(Integer updateUid) {
        this.updateUid = updateUid;
    }

    // @TableId(value = "id", type = IdType.AUTO)
    private Integer id;
    /**
     * 函数名称，给AI和让用户看的名称（需要全局唯一）
     * UNIQUE INDEX `uniq_tool_function_name`(`tool_function_name`) USING BTREE,
     */
    private String toolFunctionName;
    /**
     * 函数枚举（就是研发实现的工具类对象的方法名）
     * UNIQUE INDEX `uniq_tool_enum_function_enum`(`tool_enum`, `tool_function_enum`) USING BTREE
     */
    private String toolFunctionEnum;
    /**
     * 工具枚举（就是研发实现的工具类在spring中的beanName）
     * UNIQUE INDEX `uniq_tool_enum_function_enum`(`tool_enum`, `tool_function_enum`) USING BTREE
     */
    private String toolEnum;
    /**
     * 函数提示词（给AI用的）
     */
    private String toolFunctionDescription;
    private Date createTime;
    private Date updateTime;
    private Integer createUid;
    private Integer updateUid;

    @Override
    public String toString() {
        return id + "#" + toolEnum + "." + toolFunctionEnum + ")";
    }
}