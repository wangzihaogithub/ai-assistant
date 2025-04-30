package com.github.aiassistant.service.text.repository;

import com.github.aiassistant.entity.model.chat.*;
import com.github.aiassistant.enums.MessageTypeEnum;
import com.github.aiassistant.service.text.sseemitter.AiMessageString;
import com.github.aiassistant.util.AiUtil;
import dev.langchain4j.data.message.*;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 公用类-会话级别的记忆存储
 */
public abstract class AbstractSessionMessageRepository<MEMORY_ID, U> implements SessionMessageRepository {
    protected final RequestTrace<MEMORY_ID, U> requestTrace;
    protected AtomicInteger messageIndex;
    private volatile List<ChatMessage> historyList;
    private long startTime;
    private long firstTokenTime;

    public AbstractSessionMessageRepository(MEMORY_ID memoryId, U user, String userQueryTraceNumber, long startTime) {
        this.requestTrace = new RequestTrace<>(userQueryTraceNumber, memoryId, user, startTime);
        this.startTime = startTime;
    }

    private static <U> Message<U> convert(Message<U> last, U user, ChatMessage chatMessage, long startTime, long firstTokenTime, int baseMessageIndex) {
        Message<U> message = new Message<>();
        message.setSource(chatMessage);
        message.setParent(last);
        message.setMessageIndex(last == null ? baseMessageIndex : last.getMessageIndex() + 1);
        message.setCreateTime(new Date());
        message.setStartTime(new Date(startTime));
        message.setFirstTokenTime(firstTokenTime > 0L ? new Timestamp(firstTokenTime) : null);
        message.setUser(user);
        if (chatMessage instanceof ThinkingAiMessage) {
            ThinkingAiMessage cast = ((ThinkingAiMessage) chatMessage);
            message.setType(MessageTypeEnum.Thinking);
            message.setText(cast.text());
        } else if (chatMessage instanceof LangChainUserMessage) {
            LangChainUserMessage cast = ((LangChainUserMessage) chatMessage);
            message.setType(MessageTypeEnum.LangChainUser);
            message.setText(AiUtil.userMessageToString(cast));
        } else if (chatMessage instanceof UserMessage) {
            UserMessage cast = ((UserMessage) chatMessage);
            message.setType(MessageTypeEnum.User);
            message.setText(AiUtil.userMessageToString(cast));
        } else if (chatMessage instanceof SystemMessage) {
            SystemMessage cast = ((SystemMessage) chatMessage);
            message.setType(MessageTypeEnum.System);
            message.setText(cast.text());
        } else if (chatMessage instanceof ToolExecutionResultMessage) {
            ToolExecutionResultMessage cast = ((ToolExecutionResultMessage) chatMessage);
            message.setType(MessageTypeEnum.ToolResult);
            message.setText(cast.text());
            ToolResponse toolResponse = new ToolResponse();
            toolResponse.setRequestId(cast.id());
            toolResponse.setToolName(cast.toolName());
            message.setToolResponse(toolResponse);
        } else if (chatMessage instanceof KnowledgeAiMessage) {
            KnowledgeAiMessage cast = ((KnowledgeAiMessage) chatMessage);
            message.setType(MessageTypeEnum.Knowledge);
            message.setText(cast.text());
        } else if (chatMessage instanceof MstateAiMessage) {
            MstateAiMessage cast = ((MstateAiMessage) chatMessage);
            message.setType(MessageTypeEnum.MState);
            message.setText(cast.text());
        } else if (chatMessage instanceof AiMessage) {
            AiMessage cast = ((AiMessage) chatMessage);
            message.setType(MessageTypeEnum.Ai);
            message.setText(cast.text());
            if (cast.hasToolExecutionRequests()) {
                message.setToolRequests(cast.toolExecutionRequests().stream()
                        .map(e -> {
                            ToolRequest toolRequest = new ToolRequest();
                            toolRequest.setRequestId(e.id());
                            toolRequest.setToolName(e.name());
                            toolRequest.setArguments(e.arguments());
                            return toolRequest;
                        })
                        .collect(Collectors.toList()));
            }
        } else {
            throw new IllegalArgumentException("chatMessage is unkown");
        }
        message.setUserQueryFlag(MessageTypeEnum.User == message.getType());
        return message;
    }

    public String getUserQueryTraceNumber() {
        return requestTrace.getUserQueryTraceNumber();
    }

    protected abstract List<ChatMessage> getHistoryList(MEMORY_ID memoryId, U user);

    @Override
    public List<ChatMessage> getHistoryList() {
        if (historyList == null) {
            synchronized (this) {
                if (historyList == null) {
                    historyList = getHistoryList(requestTrace.getMemoryId(), requestTrace.getUser());
                    if (historyList == null) {
                        historyList = Collections.emptyList();
                    }
                }
            }
        }
        return historyList;
    }

    public List<ChatMessage> getRequestMessageList() {
        return requestTrace.getReadonlyMessageList().stream().map(Message::getSource).collect(Collectors.toList());
    }

    @Override
    public void afterToken(AiMessageString token) {
        if (firstTokenTime == 0) {
            this.firstTokenTime = System.currentTimeMillis();
        }
    }

    @Override
    public void add(ChatMessage message) {
        if (messageIndex == null) {
            messageIndex = new AtomicInteger(getHistoryList().size());
        }
        requestTrace.addMessage(convert(requestTrace.getLast(), requestTrace.getUser(), message, startTime, firstTokenTime, messageIndex.intValue()));
        startTime = System.currentTimeMillis();
        messageIndex.incrementAndGet();
    }

    public List<ChatMessage> getMessageList() {
        List<ChatMessage> messageList = new ArrayList<>(getHistoryList());
        for (Message<U> m : requestTrace.getReadonlyMessageList()) {
            messageList.add(m.getSource());
        }
        return messageList;
    }
}
