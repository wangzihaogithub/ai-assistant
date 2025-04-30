package com.github.aiassistant.entity;

import com.github.aiassistant.service.text.AssistantConfig;

import java.util.Date;

//// @Data
//// @TableName("ai_assistant")
public class AiAssistant implements AssistantConfig {
    //    // @TableId(value = "id", type = IdType.NONE)
    private String id;

    //    // @ApiModelProperty(value = "名称", required = true)
    private String name;

    //    // @ApiModelProperty(value = "描述", required = true)
    private String description;

    //    // @ApiModelProperty(value = "打招呼", required = true)
    private String helloMessage;

    //    // @ApiModelProperty(value = "Logo URL", required = true)
    private String logoUrl;

    // // @ApiModelProperty(value = "系统提示文本", required = true)
    private String systemPromptText;

    // // @ApiModelProperty(value = "KN提示文本", required = true)
    private String knPromptText;

    // // @ApiModelProperty(value = "状态枚举（1发布 0未发布）", required = true)
    private Integer statusEnum;

    // // @ApiModelProperty(value = "排序", required = true)
    private Integer sorted;

    // // @ApiModelProperty(value = "工具枚举", required = true)
    private String aiToolIds;

    // // @ApiModelProperty(value = "子智能体", required = true)
    private String aiJsonschemaIds;

    // // @ApiModelProperty(value = "最大记忆", required = true)
    private Integer maxMemoryTokens;

    // // @ApiModelProperty(value = "最多记忆几轮对话", required = true)
    private Integer maxMemoryRounds;

    // // @ApiModelProperty(value = "指令生成的回答中包含的最大token数。例如，如果设置为100，那么模型生成的回答中token数不会超过100个", required = true)
    private Integer maxCompletionTokens;

    private String chatApiKey;
    private String chatBaseUrl;
    private String chatModelName;

    private Date createTime;
    private Integer createUid;
    private Date updateTime;
    private Integer updateUid;

    private Double temperature;

    private String mstatePromptText;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String getTableName() {
        return "ai_assistant";
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getHelloMessage() {
        return helloMessage;
    }

    public void setHelloMessage(String helloMessage) {
        this.helloMessage = helloMessage;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    @Override
    public String getSystemPromptText() {
        return systemPromptText;
    }

    public void setSystemPromptText(String systemPromptText) {
        this.systemPromptText = systemPromptText;
    }

    public String getKnPromptText() {
        return knPromptText;
    }

    public void setKnPromptText(String knPromptText) {
        this.knPromptText = knPromptText;
    }

    public Integer getStatusEnum() {
        return statusEnum;
    }

    public void setStatusEnum(Integer statusEnum) {
        this.statusEnum = statusEnum;
    }

    public Integer getSorted() {
        return sorted;
    }

    public void setSorted(Integer sorted) {
        this.sorted = sorted;
    }

    @Override
    public String getAiToolIds() {
        return aiToolIds;
    }

    public void setAiToolIds(String aiToolIds) {
        this.aiToolIds = aiToolIds;
    }

    @Override
    public String getAiJsonschemaIds() {
        return aiJsonschemaIds;
    }

    public void setAiJsonschemaIds(String aiJsonschemaIds) {
        this.aiJsonschemaIds = aiJsonschemaIds;
    }

    @Override
    public Integer getMaxMemoryTokens() {
        return maxMemoryTokens;
    }

    public void setMaxMemoryTokens(Integer maxMemoryTokens) {
        this.maxMemoryTokens = maxMemoryTokens;
    }

    @Override
    public Integer getMaxMemoryRounds() {
        return maxMemoryRounds;
    }

    public void setMaxMemoryRounds(Integer maxMemoryRounds) {
        this.maxMemoryRounds = maxMemoryRounds;
    }

    @Override
    public Integer getMaxCompletionTokens() {
        return maxCompletionTokens;
    }

    public void setMaxCompletionTokens(Integer maxCompletionTokens) {
        this.maxCompletionTokens = maxCompletionTokens;
    }

    @Override
    public String getChatApiKey() {
        return chatApiKey;
    }

    public void setChatApiKey(String chatApiKey) {
        this.chatApiKey = chatApiKey;
    }

    @Override
    public String getChatBaseUrl() {
        return chatBaseUrl;
    }

    public void setChatBaseUrl(String chatBaseUrl) {
        this.chatBaseUrl = chatBaseUrl;
    }

    @Override
    public String getChatModelName() {
        return chatModelName;
    }

    public void setChatModelName(String chatModelName) {
        this.chatModelName = chatModelName;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Integer getCreateUid() {
        return createUid;
    }

    public void setCreateUid(Integer createUid) {
        this.createUid = createUid;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public Integer getUpdateUid() {
        return updateUid;
    }

    public void setUpdateUid(Integer updateUid) {
        this.updateUid = updateUid;
    }

    @Override
    public Double getTemperature() {
        return temperature;
    }

    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }

    public String getMstatePromptText() {
        return mstatePromptText;
    }

    public void setMstatePromptText(String mstatePromptText) {
        this.mstatePromptText = mstatePromptText;
    }

    @Override
    public String toString() {
        return id + "#" + name;
    }
}