package com.github.aiassistant.entity.model.chat;


import com.github.aiassistant.entity.model.langchain4j.MetadataAiMessage;
import com.github.aiassistant.enums.MessageTypeEnum;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;

import java.util.Date;
import java.util.List;
import java.util.Map;

public class MessageVO<U> {
    private ChatMessage source;
    private MessageVO<U> parent;
    private Integer messageIndex;
    private String text;
    private MessageTypeEnum type;
    private Boolean userQueryFlag;
    private Date createTime;
    private Date startTime;
    private Date firstTokenTime;
    private U user;
    private ToolResponseVO toolResponse;
    private List<ToolRequestVO> toolRequests;

    public String getToolRequestId() {
        if (source instanceof ToolExecutionResultMessage) {
            return ((ToolExecutionResultMessage) source).id();
        } else {
            return null;
        }
    }

//    public List<KnJobVO> getJobList() {
//        if (source instanceof FindJobToolExecutionResultMessage) {
//            return ((FindJobToolExecutionResultMessage) source).getJobList();
//        } else {
//            return null;
//        }
//    }

    public String getOpenAiRequestId() {
        if (source instanceof MetadataAiMessage) {
            return ((MetadataAiMessage) source).getOpenAiRequestId();
        } else {
            return null;
        }
    }

    public String getMemoryString() {
        if (source instanceof MetadataAiMessage) {
            return ((MetadataAiMessage) source).getMemoryString();
        } else {
            return null;
        }
    }

    public List<Map<String, Object>> getStringMetaMapList() {
        if (source instanceof MetadataAiMessage) {
            return ((MetadataAiMessage) source).getStringMetaMapList();
        } else {
            return null;
        }
    }

    public int getTotalTokenCount() {
        if (source instanceof MetadataAiMessage) {
            return ((MetadataAiMessage) source).getTotalTokenCount();
        } else {
            return 0;
        }
    }

    public int getInputTokenCount() {
        if (source instanceof MetadataAiMessage) {
            return ((MetadataAiMessage) source).getInputTokenCount();
        } else {
            return 0;
        }
    }

    public int getOutputTokenCount() {
        if (source instanceof MetadataAiMessage) {
            return ((MetadataAiMessage) source).getOutputTokenCount();
        } else {
            return 0;
        }
    }

    public ChatMessage getSource() {
        return source;
    }

    public void setSource(ChatMessage source) {
        this.source = source;
    }

    public MessageVO<U> getParent() {
        return parent;
    }

    public void setParent(MessageVO<U> parent) {
        this.parent = parent;
    }

    public Integer getMessageIndex() {
        return messageIndex;
    }

    public void setMessageIndex(Integer messageIndex) {
        this.messageIndex = messageIndex;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public MessageTypeEnum getType() {
        return type;
    }

    public void setType(MessageTypeEnum type) {
        this.type = type;
    }

    public Boolean getUserQueryFlag() {
        return userQueryFlag;
    }

    public void setUserQueryFlag(Boolean userQueryFlag) {
        this.userQueryFlag = userQueryFlag;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getFirstTokenTime() {
        return firstTokenTime;
    }

    public void setFirstTokenTime(Date firstTokenTime) {
        this.firstTokenTime = firstTokenTime;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public U getUser() {
        return user;
    }

    public void setUser(U user) {
        this.user = user;
    }

    public ToolResponseVO getToolResponse() {
        return toolResponse;
    }

    public void setToolResponse(ToolResponseVO toolResponse) {
        this.toolResponse = toolResponse;
    }

    public List<ToolRequestVO> getToolRequests() {
        return toolRequests;
    }

    public void setToolRequests(List<ToolRequestVO> toolRequests) {
        this.toolRequests = toolRequests;
    }

    @Override
    public String toString() {
        return type + "(" + text + ")";
    }
}
