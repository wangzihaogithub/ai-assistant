package com.github.aiassistant.entity;

import java.io.Serializable;

// @ApiModel(value = "AiChatWebsearchResult", description = "联网结果")
// @Data
// @TableName("ai_chat_websearch_result")
public class AiChatWebsearchResult implements Serializable {
    // @TableId(value = "id", type = IdType.AUTO)
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
     * 联网搜索id
     */
    private Integer aiChatWebsearchId;
    /**
     * 网页url
     */
    private String pageUrl;
    /**
     * 网页标题
     */
    private String pageTitle;
    /**
     * 网页时间
     */
    private String pageTime;
    /**
     * 网页来源
     */
    private String pageSource;
    /**
     * 网页正文
     */
    private String pageContent;
    /**
     * 读取内容耗时
     */
    private Long urlReadTimeCost;
    /**
     * 读取内容的代理
     */
    private String urlReadProxy;

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

    public Integer getAiChatWebsearchId() {
        return aiChatWebsearchId;
    }

    public void setAiChatWebsearchId(Integer aiChatWebsearchId) {
        this.aiChatWebsearchId = aiChatWebsearchId;
    }

    public String getPageUrl() {
        return pageUrl;
    }

    public void setPageUrl(String pageUrl) {
        this.pageUrl = pageUrl;
    }

    public String getPageTitle() {
        return pageTitle;
    }

    public void setPageTitle(String pageTitle) {
        this.pageTitle = pageTitle;
    }

    public String getPageTime() {
        return pageTime;
    }

    public void setPageTime(String pageTime) {
        this.pageTime = pageTime;
    }

    public String getPageSource() {
        return pageSource;
    }

    public void setPageSource(String pageSource) {
        this.pageSource = pageSource;
    }

    public String getPageContent() {
        return pageContent;
    }

    public void setPageContent(String pageContent) {
        this.pageContent = pageContent;
    }

    public Long getUrlReadTimeCost() {
        return urlReadTimeCost;
    }

    public void setUrlReadTimeCost(Long urlReadTimeCost) {
        this.urlReadTimeCost = urlReadTimeCost;
    }

    public String getUrlReadProxy() {
        return urlReadProxy;
    }

    public void setUrlReadProxy(String urlReadProxy) {
        this.urlReadProxy = urlReadProxy;
    }

    @Override
    public String toString() {
        return id + "#" + pageTitle;
    }
}