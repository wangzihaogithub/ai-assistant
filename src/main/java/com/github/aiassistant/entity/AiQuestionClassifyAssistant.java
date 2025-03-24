package com.github.aiassistant.entity;

import com.github.aiassistant.service.text.AssistantConfig;

// @Data
// @TableName("ai_question_classify_assistant")
public class AiQuestionClassifyAssistant implements AssistantConfig {
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getSystemPromptText() {
        return systemPromptText;
    }

    public void setSystemPromptText(String systemPromptText) {
        this.systemPromptText = systemPromptText;
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

    @Override
    public Double getTemperature() {
        return temperature;
    }

    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }

    // @TableId(value = "id", type = IdType.NONE)
    private Integer id;

    // @ApiModelProperty(value = "名称", required = true)
    private String name;

    // @ApiModelProperty(value = "系统提示文本", required = true)
    private String systemPromptText;

    // @ApiModelProperty(value = "工具枚举", required = true)
    private String aiToolIds;

    // @ApiModelProperty(value = "子智能体", required = true)
    private String aiJsonschemaIds;

    // @ApiModelProperty(value = "最大记忆", required = true)
    private Integer maxMemoryTokens;

    // @ApiModelProperty(value = "最多记忆几轮对话", required = true)
    private Integer maxMemoryRounds;

    // @ApiModelProperty(value = "指令生成的回答中包含的最大token数。例如，如果设置为100，那么模型生成的回答中token数不会超过100个", required = true)
    private Integer maxCompletionTokens;

    private String chatApiKey;
    private String chatBaseUrl;
    private String chatModelName;

    private Double temperature;

}