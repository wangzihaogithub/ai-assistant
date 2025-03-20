package com.github.aiassistant.entity.model.chat;

import java.sql.Timestamp;
import java.util.*;

/**
 * 大模型请求追踪
 */
public class RequestTrace<MEMORY_ID, U> {
    /**
     * 追踪用户请求
     */
    private final String userQueryTraceNumber;
    private final MEMORY_ID memoryId;
    private final U user;
    private final Date createTime;
    private final List<Message<U>> requestList = new ArrayList<>();
    private final List<Message<U>> responseList = new ArrayList<>();
    private List<List<QaKnVO>> requestKnowledgeList = new ArrayList<>();
    private Message<U> last;
    private StageEnum stageEnum = StageEnum.Request;

    public RequestTrace(String userQueryTraceNumber, MEMORY_ID memoryId, U user, long startTime) {
        this.userQueryTraceNumber = userQueryTraceNumber;
        this.memoryId = memoryId;
        this.createTime = new Timestamp(startTime);
        this.user = user;
    }

    public static boolean isStageRequest(String stageEnumKey) {
        return Objects.equals(stageEnumKey, StageEnum.Request.key);
    }

    public String getUserQueryTraceNumber() {
        return userQueryTraceNumber;
    }

    public List<List<QaKnVO>> getRequestKnowledgeList() {
        return requestKnowledgeList;
    }

    public void setRequestKnowledgeList(List<List<QaKnVO>> knowledgeTextContentList) {
        this.requestKnowledgeList = knowledgeTextContentList;
    }

    public List<Message<U>> getRequestMessageList() {
        return Collections.unmodifiableList(requestList);
    }

    public List<Message<U>> getResponseMessageList() {
        return Collections.unmodifiableList(responseList);
    }

    public List<Message<U>> getReadonlyMessageList() {
        List<Message<U>> list = new ArrayList<>(requestList.size() + responseList.size());
        list.addAll(requestList);
        list.addAll(responseList);
        return Collections.unmodifiableList(list);
    }

    public int getMessageSize() {
        return requestList.size() + responseList.size();
    }

    public Date getCreateTime() {
        return createTime;
    }

    public MEMORY_ID getMemoryId() {
        return memoryId;
    }

    public U getUser() {
        return user;
    }

    public void changeToResponse() {
        this.stageEnum = StageEnum.Response;
        this.requestKnowledgeList = new ArrayList<>();
    }

    public Message<U> getLast() {
        return last;
    }

    public void addMessage(Message<U> message) {
        this.last = message;
        if (stageEnum == StageEnum.Request) {
            requestList.add(message);
        } else {
            responseList.add(message);
        }
    }

    public boolean isStageRequest() {
        return isStageRequest(stageEnum.key);
    }

    public String getStageEnumKey() {
        return stageEnum.key;
    }

    private enum StageEnum {
        Request("Request"),
        Response("Response");
        final String key;

        StageEnum(String key) {
            this.key = key;
        }
    }
}
