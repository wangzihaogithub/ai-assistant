package com.github.aiassistant.entity;

// import com.baomidou.mybatisplus.annotation.IdType;
// import com.baomidou.mybatisplus.annotation.TableId;
// import com.baomidou.mybatisplus.annotation.TableName;
// import io.swagger.annotations.ApiModelProperty;
// import lombok.Data;

// @Data
// @TableName("ai_question_classify")
public class AiQuestionClassify {
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getReadTimeoutSecond() {
        return readTimeoutSecond;
    }

    public void setReadTimeoutSecond(Integer readTimeoutSecond) {
        this.readTimeoutSecond = readTimeoutSecond;
    }

    public String getClassifyName() {
        return classifyName;
    }

    public void setClassifyName(String classifyName) {
        this.classifyName = classifyName;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getGroupCode() {
        return groupCode;
    }

    public void setGroupCode(String groupCode) {
        this.groupCode = groupCode;
    }

    public String getActionEnums() {
        return actionEnums;
    }

    public void setActionEnums(String actionEnums) {
        this.actionEnums = actionEnums;
    }

    public String getExampleText() {
        return exampleText;
    }

    public void setExampleText(String exampleText) {
        this.exampleText = exampleText;
    }

    public Boolean getEnableFlag() {
        return enableFlag;
    }

    public void setEnableFlag(Boolean enableFlag) {
        this.enableFlag = enableFlag;
    }

    public String getAiAssistantId() {
        return aiAssistantId;
    }

    public void setAiAssistantId(String aiAssistantId) {
        this.aiAssistantId = aiAssistantId;
    }

    public Integer getAiQuestionClassifyAssistantId() {
        return aiQuestionClassifyAssistantId;
    }

    public void setAiQuestionClassifyAssistantId(Integer aiQuestionClassifyAssistantId) {
        this.aiQuestionClassifyAssistantId = aiQuestionClassifyAssistantId;
    }

    // @TableId(value = "id", type = IdType.NONE)
    private Integer id;

    /**
     * 请求超时秒数
     */
    private Integer readTimeoutSecond;
    // @ApiModelProperty(value = "问题类型名称（二级）", required = true)
    private String classifyName;

    // @ApiModelProperty(value = "问题类型名称（一级）", required = true)
    private String groupName;

    // @ApiModelProperty(value = "问题类型代码（一级）", required = true)
    private String groupCode;

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

    // @ApiModelProperty(value = "示例文案", required = true)
    private String exampleText;

    // @ApiModelProperty(value = "是否开启", required = true)
    private Boolean enableFlag;

    /**
     * 智能体ID
     */
    private String aiAssistantId;

    /**
     * 分配AI
     */
    private Integer aiQuestionClassifyAssistantId;

}