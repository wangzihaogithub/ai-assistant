package com.github.aiassistant.entity.model.chat;


import com.github.aiassistant.enums.MessageTypeEnum;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;

import java.util.Date;
import java.util.List;

public class Message<U> {
    private ChatMessage source;
    private Message<U> parent;
    private Integer messageIndex;
    private String text;
    private MessageTypeEnum type;
    private Boolean userQueryFlag;
    private Date createTime;
    private Date startTime;
    private Date firstTokenTime;
    private U user;
    private ToolResponse toolResponse;
    private List<ToolRequest> toolRequests;

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

    public KnowledgeTextContent getKnowledgeTextContent() {
        if (source instanceof KnowledgeAiMessage) {
            return ((KnowledgeAiMessage) source).getKnowledgeTextContent();
        } else {
            return null;
        }
    }

    public String getOpenAiRequestId() {
        if (source instanceof IDAiMessage) {
            return ((IDAiMessage) source).getId();
        } else {
            return null;
        }
    }

    public ChatMessage getSource() {
        return source;
    }

    public void setSource(ChatMessage source) {
        this.source = source;
    }

    public Message<U> getParent() {
        return parent;
    }

    public void setParent(Message<U> parent) {
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

    public ToolResponse getToolResponse() {
        return toolResponse;
    }

    public void setToolResponse(ToolResponse toolResponse) {
        this.toolResponse = toolResponse;
    }

    public List<ToolRequest> getToolRequests() {
        return toolRequests;
    }

    public void setToolRequests(List<ToolRequest> toolRequests) {
        this.toolRequests = toolRequests;
    }
}
