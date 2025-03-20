package com.github.aiassistant.entity;

// import com.baomidou.mybatisplus.annotation.IdType;
// import com.baomidou.mybatisplus.annotation.TableId;
// import com.baomidou.mybatisplus.annotation.TableName;
// import io.swagger.annotations.ApiModel;
// import lombok.Data;

import java.util.Date;

// @ApiModel(value = "AiChatWebsearch", description = "联网")
// @Data
// @TableName("ai_chat_websearch")
public class AiChatWebsearch {
    public String getUserQueryTraceNumber() {
        return userQueryTraceNumber;
    }

    public void setUserQueryTraceNumber(String userQueryTraceNumber) {
        this.userQueryTraceNumber = userQueryTraceNumber;
    }

    public String getSearchProxy() {
        return searchProxy;
    }

    public void setSearchProxy(String searchProxy) {
        this.searchProxy = searchProxy;
    }

    public Long getSearchTimeMs() {
        return searchTimeMs;
    }

    public void setSearchTimeMs(Long searchTimeMs) {
        this.searchTimeMs = searchTimeMs;
    }

    public String getProviderName() {
        return providerName;
    }

    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getSourceEnum() {
        return sourceEnum;
    }

    public void setSourceEnum(String sourceEnum) {
        this.sourceEnum = sourceEnum;
    }

    public Integer getUserChatHistoryId() {
        return userChatHistoryId;
    }

    public void setUserChatHistoryId(Integer userChatHistoryId) {
        this.userChatHistoryId = userChatHistoryId;
    }

    public Integer getAiChatId() {
        return aiChatId;
    }

    public void setAiChatId(Integer aiChatId) {
        this.aiChatId = aiChatId;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

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
}