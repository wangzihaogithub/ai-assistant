package com.github.aiassistant.entity.model.chat;

import java.util.Date;

// @Data
public class AiChatWebsearchResp {
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getAiChatId() {
        return aiChatId;
    }

    public void setAiChatId(Integer aiChatId) {
        this.aiChatId = aiChatId;
    }

    public Integer getUserChatHistoryId() {
        return userChatHistoryId;
    }

    public void setUserChatHistoryId(Integer userChatHistoryId) {
        this.userChatHistoryId = userChatHistoryId;
    }

    public String getSourceEnum() {
        return sourceEnum;
    }

    public void setSourceEnum(String sourceEnum) {
        this.sourceEnum = sourceEnum;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public String getProviderName() {
        return providerName;
    }

    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }

    public Long getSearchTimeMs() {
        return searchTimeMs;
    }

    public void setSearchTimeMs(Long searchTimeMs) {
        this.searchTimeMs = searchTimeMs;
    }

    public String getSearchProxy() {
        return searchProxy;
    }

    public void setSearchProxy(String searchProxy) {
        this.searchProxy = searchProxy;
    }

    public String getUserQueryTraceNumber() {
        return userQueryTraceNumber;
    }

    public void setUserQueryTraceNumber(String userQueryTraceNumber) {
        this.userQueryTraceNumber = userQueryTraceNumber;
    }

    public Integer getResultCount() {
        return resultCount;
    }

    public void setResultCount(Integer resultCount) {
        this.resultCount = resultCount;
    }

    private Integer id;

    /**
     * 聊天ID
     */
    private Integer aiChatId;
    /**
     * 用户提问的消息ID
     */
    private Integer userChatHistoryId;
    /**
     * 触发来源
     */
    private String sourceEnum;
    /**
     * 联网的问题
     */
    private String question;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 供应商
     */
    private String providerName;
    /**
     * 查询耗时
     */
    private Long searchTimeMs;
    /**
     * 搜索用的代理
     */
    private String searchProxy;

    /**
     * 每次用户请求的唯一序号
     */
    private String userQueryTraceNumber;
    /**
     * 结果数量
     */
    private Integer resultCount;
}
