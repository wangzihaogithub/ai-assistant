package com.github.aiassistant.entity;


import java.io.Serializable;

//// @ApiModel(value = "AiAssistantFewshot", description = "少样本学习")
//// @Data
//// @TableName("ai_assistant_fewshot")
public class AiAssistantFewshot implements Serializable {

    //    // @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 智能体ID
     */
    private String aiAssistantId;

    /**
     * 消息下标
     */
    private Integer messageIndex;

    /**
     * 消息内容
     */
    private String messageText;

    /**
     * 消息类型枚举: 参考MessageTypeEnum枚举
     * 取值范围【User，Ai】
     */
    private String messageTypeEnum;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getAiAssistantId() {
        return aiAssistantId;
    }

    public void setAiAssistantId(String aiAssistantId) {
        this.aiAssistantId = aiAssistantId;
    }

    public Integer getMessageIndex() {
        return messageIndex;
    }

    public void setMessageIndex(Integer messageIndex) {
        this.messageIndex = messageIndex;
    }

    public String getMessageText() {
        return messageText;
    }

    public void setMessageText(String messageText) {
        this.messageText = messageText;
    }

    public String getMessageTypeEnum() {
        return messageTypeEnum;
    }

    public void setMessageTypeEnum(String messageTypeEnum) {
        this.messageTypeEnum = messageTypeEnum;
    }

    @Override
    public String toString() {
        return id + "#" + aiAssistantId + "." + messageText;
    }
}