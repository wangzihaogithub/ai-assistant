package com.github.aiassistant.entity;

import java.io.Serializable;

// @ApiModel(value = "AiChatReasoningPlan", description = "思考计划")
// @Data
// @TableName("ai_chat_reasoning_plan")
public class AiChatReasoningPlan implements Serializable {
    // @TableId(value = "id", type = IdType.AUTO)
    private Integer id;
    /**
     * 思考ID
     */
    private Integer aiChatReasoningId;
    /**
     * 聊天ID
     */
    private Integer aiChatId;
    /**
     * 用户提问的消息ID
     */
    private Integer userChatHistoryId;
    /**
     * 任务
     */
    private String task;
    /**
     * 此字段用于你向用户解释，没有解决的原因
     */
    private String failMessage;
    /**
     * 如果已被解决，这就是解决的最终答案
     */
    private String answer;
    /**
     * 如果你有不明白或需要向用户确认的问题，可以通过此字段向用户提问
     */
    private String aiQuestion;
    /**
     * 此字段用于标识这个任务是否被解决
     */
    private Boolean resolvedFlag;
    /**
     * 第几个计划下标
     */
    private Integer planIndex;
    /**
     * 如果任务未被解决，可以在此字段上返回一些搜索关键词，以助于使用搜索引擎
     */
    private String websearchKeyword;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getAiChatReasoningId() {
        return aiChatReasoningId;
    }

    public void setAiChatReasoningId(Integer aiChatReasoningId) {
        this.aiChatReasoningId = aiChatReasoningId;
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

    public String getTask() {
        return task;
    }

    public void setTask(String task) {
        this.task = task;
    }

    public String getFailMessage() {
        return failMessage;
    }

    public void setFailMessage(String failMessage) {
        this.failMessage = failMessage;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public String getAiQuestion() {
        return aiQuestion;
    }

    public void setAiQuestion(String aiQuestion) {
        this.aiQuestion = aiQuestion;
    }

    public Boolean getResolvedFlag() {
        return resolvedFlag;
    }

    public void setResolvedFlag(Boolean resolvedFlag) {
        this.resolvedFlag = resolvedFlag;
    }

    public Integer getPlanIndex() {
        return planIndex;
    }

    public void setPlanIndex(Integer planIndex) {
        this.planIndex = planIndex;
    }

    public String getWebsearchKeyword() {
        return websearchKeyword;
    }

    public void setWebsearchKeyword(String websearchKeyword) {
        this.websearchKeyword = websearchKeyword;
    }

    @Override
    public String toString() {
        return id + "#" + task;
    }
}