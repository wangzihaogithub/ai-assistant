package com.github.aiassistant.entity;

// import com.baomidou.mybatisplus.annotation.IdType;
// import com.baomidou.mybatisplus.annotation.TableId;
// import com.baomidou.mybatisplus.annotation.TableName;
// import io.swagger.annotations.ApiModel;
// import lombok.Data;

import java.util.Date;

// @ApiModel(value = "AiChatReasoning", description = "思考")
// @Data
// @TableName("ai_chat_reasoning")
public class AiChatReasoning {
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

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public Boolean getNeedSplittingFlag() {
        return needSplittingFlag;
    }

    public void setNeedSplittingFlag(Boolean needSplittingFlag) {
        this.needSplittingFlag = needSplittingFlag;
    }

    public Integer getUserChatHistoryId() {
        return userChatHistoryId;
    }

    public void setUserChatHistoryId(Integer userChatHistoryId) {
        this.userChatHistoryId = userChatHistoryId;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public String getUserQueryTraceNumber() {
        return userQueryTraceNumber;
    }

    public void setUserQueryTraceNumber(String userQueryTraceNumber) {
        this.userQueryTraceNumber = userQueryTraceNumber;
    }

    // @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 聊天ID
     */
    private Integer aiChatId;

    /**
     * 问题
     */
    private String question;

    /**
     * 是否需要问题拆分
     */
    private Boolean needSplittingFlag;

    /**
     * 用户提问的消息ID
     */
    private Integer userChatHistoryId;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 每次用户请求的唯一序号
     */
    private String userQueryTraceNumber;
}