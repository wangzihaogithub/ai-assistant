package com.github.aiassistant.entity;

import java.util.Date;

// @Data
// @ApiModel(value = "AiChatClassify", description = "聊天分类")
// @TableName("ai_chat_classify")
public class AiChatClassify {
    // @TableId(value = "id", type = IdType.AUTO)
    // @ApiModelProperty(value = "ID", example = "1")
    private Integer id;
    // @ApiModelProperty(value = "创建时间", example = "2023-01-01T00:00:00")
    private Date createTime;
    // @ApiModelProperty(value = "聊天ID", example = "1")
    private Integer aiChatId;
    // @ApiModelProperty(value = "用户问题聊天追踪号", example = "101")
    private String userQueryTraceNumber;
    // @ApiModelProperty(value = "分类id（ai_question_classify表主键）", example = "1")
    private Integer classifyId;
    // @ApiModelProperty(value = "分类名称（ai_question_classify表name）", example = "101")
    private String classifyName;
    // @ApiModelProperty(value = "分类名称（ai_question_classify表name）", example = "101")
    private String classifyGroupCode;
    // @ApiModelProperty(value = "分类分组（ai_question_classify表group_name）", example = "101")
    private String classifyGroupName;
    /**
     * 用户问题
     */
    private String question;
    /**
     * 分配AI
     */
    private Integer aiQuestionClassifyAssistantId;
    // @ApiModelProperty(value = "控制的动作,多个用逗号分隔\n" +
//            "qa= 问答库\n" +
//            "jdlw = 简单联网\n" +
//            "dclw = 多层联网\n" +
//            "lwdd = 联网兜底\n" +
//            "wtcj = 问题拆解\n" +
//            "\n" +
//            " enum AiQuestionClassifyActionEnum {\n" +
//            "    qa(\"qa\", \"问答库\"),\n" +
//            "    jdlw(\"jdlw\", \"简单联网\"),\n" +
//            "wtcj(\"wtcj\", \"问题拆解\"),\n" +
//            "    dclw(\"dclw\", \"多层联网\"),\n" +
//            "    lwdd(\"lwdd\", \"联网兜底\");\n" +
//            "\n", required = true)
    private String actionEnums;

    public String getActionEnums() {
        return actionEnums;
    }

    public void setActionEnums(String actionEnums) {
        this.actionEnums = actionEnums;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Integer getAiChatId() {
        return aiChatId;
    }

    public void setAiChatId(Integer aiChatId) {
        this.aiChatId = aiChatId;
    }

    public String getUserQueryTraceNumber() {
        return userQueryTraceNumber;
    }

    public void setUserQueryTraceNumber(String userQueryTraceNumber) {
        this.userQueryTraceNumber = userQueryTraceNumber;
    }

    public Integer getClassifyId() {
        return classifyId;
    }

    public void setClassifyId(Integer classifyId) {
        this.classifyId = classifyId;
    }

    public String getClassifyName() {
        return classifyName;
    }

    public void setClassifyName(String classifyName) {
        this.classifyName = classifyName;
    }

    public String getClassifyGroupCode() {
        return classifyGroupCode;
    }

    public void setClassifyGroupCode(String classifyGroupCode) {
        this.classifyGroupCode = classifyGroupCode;
    }

    public String getClassifyGroupName() {
        return classifyGroupName;
    }

    public void setClassifyGroupName(String classifyGroupName) {
        this.classifyGroupName = classifyGroupName;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public Integer getAiQuestionClassifyAssistantId() {
        return aiQuestionClassifyAssistantId;
    }

    public void setAiQuestionClassifyAssistantId(Integer aiQuestionClassifyAssistantId) {
        this.aiQuestionClassifyAssistantId = aiQuestionClassifyAssistantId;
    }

    @Override
    public String toString() {
        return classifyName + "#" + question;
    }
}