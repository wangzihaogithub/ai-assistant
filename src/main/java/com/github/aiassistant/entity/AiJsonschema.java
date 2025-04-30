package com.github.aiassistant.entity;

//// @Data
//// @TableName("ai_jsonschema")
public class AiJsonschema {

    //    // @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    private String apiKey;
    private String baseUrl;
    private String modelName;

    private String responseFormat;


    private String jsonSchemaEnum;
    private String systemPromptText;
    private String userPromptText;

    private String aiToolIds;
    private Boolean enableFlag;

    /**
     * 指令生成的回答中包含的最大token数。例如，如果设置为100，那么模型生成的回答中token数不会超过100个
     */
    private Integer maxCompletionTokens;

    /**
     * 丰富度，0.1至1之间，最大越丰富
     */
    private Double temperature;
    /**
     * 随机度，0.1至1之间，最大越随机
     */
    private Double topP;

    /**
     * 备注
     */
    private String remark;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public String getResponseFormat() {
        return responseFormat;
    }

    public void setResponseFormat(String responseFormat) {
        this.responseFormat = responseFormat;
    }

    public String getJsonSchemaEnum() {
        return jsonSchemaEnum;
    }

    public void setJsonSchemaEnum(String jsonSchemaEnum) {
        this.jsonSchemaEnum = jsonSchemaEnum;
    }

    public String getSystemPromptText() {
        return systemPromptText;
    }

    public void setSystemPromptText(String systemPromptText) {
        this.systemPromptText = systemPromptText;
    }

    public String getUserPromptText() {
        return userPromptText;
    }

    public void setUserPromptText(String userPromptText) {
        this.userPromptText = userPromptText;
    }

    public String getAiToolIds() {
        return aiToolIds;
    }

    public void setAiToolIds(String aiToolIds) {
        this.aiToolIds = aiToolIds;
    }

    public Boolean getEnableFlag() {
        return enableFlag;
    }

    public void setEnableFlag(Boolean enableFlag) {
        this.enableFlag = enableFlag;
    }

    public Integer getMaxCompletionTokens() {
        return maxCompletionTokens;
    }

    public void setMaxCompletionTokens(Integer maxCompletionTokens) {
        this.maxCompletionTokens = maxCompletionTokens;
    }

    public Double getTemperature() {
        return temperature;
    }

    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }

    public Double getTopP() {
        return topP;
    }

    public void setTopP(Double topP) {
        this.topP = topP;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    @Override
    public String toString() {
        return id + "#" + jsonSchemaEnum;
    }
}