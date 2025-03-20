package com.github.aiassistant.entity;

// import com.baomidou.mybatisplus.annotation.TableId;
// import com.baomidou.mybatisplus.annotation.TableName;
// import io.swagger.annotations.ApiModel;
// import io.swagger.annotations.ApiModelProperty;
// import lombok.Data;

// import com.baomidou.mybatisplus.annotation.IdType;
// @Data
// @ApiModel(value = "AiMemoryMessageTool", description = "记忆消息中借助了工具")
// @TableName("ai_memory_message_tool")
public class AiMemoryMessageTool {

     // @TableId(value = "id", type = IdType.AUTO)
    // @ApiModelProperty(value = "ID", example = "1")
    private Integer id;

    // @ApiModelProperty(value = "记忆消息ID", example = "1")
    private Integer aiMemoryMessageId;

    // @ApiModelProperty(value = "工具请求ID", example = "toolRequestId123")
    private String toolRequestId;

    // @ApiModelProperty(value = "工具名称", example = "搜索引擎")
    private String toolName;

    // @ApiModelProperty(value = "工具参数", example = "{\"key\":\"value\"}")
    private String toolArguments;

    // @ApiModelProperty(value = "记忆ID", example = "1")
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

    public String getToolRequestId() {
        return toolRequestId;
    }

    public void setToolRequestId(String toolRequestId) {
        this.toolRequestId = toolRequestId;
    }

    public String getToolName() {
        return toolName;
    }

    public void setToolName(String toolName) {
        this.toolName = toolName;
    }

    public String getToolArguments() {
        return toolArguments;
    }

    public void setToolArguments(String toolArguments) {
        this.toolArguments = toolArguments;
    }

    public Integer getAiMemoryId() {
        return aiMemoryId;
    }

    public void setAiMemoryId(Integer aiMemoryId) {
        this.aiMemoryId = aiMemoryId;
    }
}