package com.github.aiassistant.entity;

import java.util.Date;

// @ApiModel(value = "AiMemorySearch", description = "记忆的RAG记录")
// @Data
// @TableName("ai_memory_search")
public class AiMemorySearch {
    // @TableId(value = "id", type = IdType.AUTO)
    private Integer id;
    /**
     * 记忆ID
     */
    private Integer aiMemoryId;
    /**
     * 查询索引对象
     */
    private String indexName;
    /**
     * 聊天ID
     */
    private Integer aiChatId;
    /**
     * 查询条件
     */
    private String requestBody;
    /**
     * 返回文档数量
     */
    private Integer responseDocCount;
    /**
     * 错误内容
     */
    private String errorMessage;
    /**
     * 本次提问的问题追踪ID
     */
    private String userQueryTraceNumber;

    /**
     * 开始时间
     */
    private Date searchStartTime;

    /**
     * 花费毫秒
     */
    private Integer searchCostMs;
    /**
     * 结束时间
     */
    private Date searchEndTime;

    /**
     * 创建时间
     */
    private Date createTime;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getAiMemoryId() {
        return aiMemoryId;
    }

    public void setAiMemoryId(Integer aiMemoryId) {
        this.aiMemoryId = aiMemoryId;
    }

    public String getIndexName() {
        return indexName;
    }

    public void setIndexName(String indexName) {
        this.indexName = indexName;
    }

    public Integer getAiChatId() {
        return aiChatId;
    }

    public void setAiChatId(Integer aiChatId) {
        this.aiChatId = aiChatId;
    }

    public String getRequestBody() {
        return requestBody;
    }

    public void setRequestBody(String requestBody) {
        this.requestBody = requestBody;
    }

    public Integer getResponseDocCount() {
        return responseDocCount;
    }

    public void setResponseDocCount(Integer responseDocCount) {
        this.responseDocCount = responseDocCount;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getUserQueryTraceNumber() {
        return userQueryTraceNumber;
    }

    public void setUserQueryTraceNumber(String userQueryTraceNumber) {
        this.userQueryTraceNumber = userQueryTraceNumber;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getSearchStartTime() {
        return searchStartTime;
    }

    public void setSearchStartTime(Date searchStartTime) {
        this.searchStartTime = searchStartTime;
    }

    public Date getSearchEndTime() {
        return searchEndTime;
    }

    public void setSearchEndTime(Date searchEndTime) {
        this.searchEndTime = searchEndTime;
    }

    public Integer getSearchCostMs() {
        return searchCostMs;
    }

    public void setSearchCostMs(Integer searchCostMs) {
        this.searchCostMs = searchCostMs;
    }

    @Override
    public String toString() {
        return id + "#" + indexName + "(" + responseDocCount + ")";
    }
}