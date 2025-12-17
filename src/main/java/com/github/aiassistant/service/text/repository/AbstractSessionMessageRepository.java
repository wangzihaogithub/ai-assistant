package com.github.aiassistant.service.text.repository;

import com.github.aiassistant.entity.model.chat.MessageVO;
import com.github.aiassistant.entity.model.chat.RequestTraceVO;
import com.github.aiassistant.entity.model.chat.ToolRequestVO;
import com.github.aiassistant.entity.model.chat.ToolResponseVO;
import com.github.aiassistant.entity.model.langchain4j.KnowledgeAiMessage;
import com.github.aiassistant.entity.model.langchain4j.MetadataAiMessage;
import com.github.aiassistant.entity.model.langchain4j.MstateAiMessage;
import com.github.aiassistant.entity.model.langchain4j.ThinkingAiMessage;
import com.github.aiassistant.enums.MessageTypeEnum;
import com.github.aiassistant.service.text.sseemitter.AiMessageString;
import com.github.aiassistant.util.AiUtil;
import dev.langchain4j.data.message.*;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 公用类-会话级别的记忆存储
 */
public abstract class AbstractSessionMessageRepository<MEMORY_ID, U> implements SessionMessageRepository {
    protected final RequestTraceVO<MEMORY_ID, U> requestTrace;
    protected AtomicInteger messageIndex;
    private volatile List<ChatMessage> historyList;
    private long startTime;
    private long firstTokenTime;

    public AbstractSessionMessageRepository(MEMORY_ID memoryId, U user, String userQueryTraceNumber, long startTime) {
        this.requestTrace = new RequestTraceVO<>(userQueryTraceNumber, memoryId, user, startTime);
        this.startTime = startTime;
    }

    private static <U> MessageVO<U> convert(MessageVO<U> last, U user, ChatMessage chatMessage, long startTime, long firstTokenTime, int baseMessageIndex) {
        MessageVO<U> message = new MessageVO<>();
        message.setSource(chatMessage);
        message.setParent(last);
        message.setMessageIndex(last == null ? baseMessageIndex : last.getMessageIndex() + 1);
        message.setCreateTime(new Date());
        message.setStartTime(new Date(startTime));
        message.setFirstTokenTime(firstTokenTime > 0L ? new Timestamp(firstTokenTime) : null);
        message.setUser(user);
        ChatMessage source;
        if (chatMessage instanceof MetadataAiMessage) {
            source = ((MetadataAiMessage) chatMessage).getResponse().content();
        } else {
            source = chatMessage;
        }
        if (source instanceof UserMessage) {
            UserMessage cast = ((UserMessage) source);
            message.setText(AiUtil.userMessageToString(cast));
            if (AiUtil.isTypeUser(source)) {
                message.setType(MessageTypeEnum.User);
            } else {
                message.setType(MessageTypeEnum.LangChainUser);
            }
        } else if (source instanceof SystemMessage) {
            SystemMessage cast = ((SystemMessage) source);
            message.setType(MessageTypeEnum.System);
            message.setText(cast.text());
        } else if (source instanceof ToolExecutionResultMessage) {
            ToolExecutionResultMessage cast = ((ToolExecutionResultMessage) source);
            message.setType(MessageTypeEnum.ToolResult);
            message.setText(cast.text());
            ToolResponseVO toolResponse = new ToolResponseVO();
            toolResponse.setRequestId(cast.id());
            toolResponse.setToolName(cast.toolName());
            message.setToolResponse(toolResponse);
        } else if (source instanceof KnowledgeAiMessage) {
            KnowledgeAiMessage cast = ((KnowledgeAiMessage) source);
            message.setType(MessageTypeEnum.Knowledge);
            message.setText(cast.text());
        } else if (source instanceof MstateAiMessage) {
            MstateAiMessage cast = ((MstateAiMessage) source);
            message.setType(MessageTypeEnum.MState);
            message.setText(cast.text());
        } else if (source instanceof ThinkingAiMessage) {
            ThinkingAiMessage cast = ((ThinkingAiMessage) source);
            message.setType(MessageTypeEnum.Thinking);
            message.setText(cast.text());
            if (cast.hasToolExecutionRequests()) {
                message.setToolRequests(cast.toolExecutionRequests().stream()
                        .map(e -> {
                            ToolRequestVO toolRequest = new ToolRequestVO();
                            toolRequest.setRequestId(e.id());
                            toolRequest.setToolName(e.name());
                            toolRequest.setArguments(e.arguments());
                            return toolRequest;
                        })
                        .collect(Collectors.toList()));
            }
        } else if (source instanceof AiMessage) {
            AiMessage cast = ((AiMessage) source);
            if (AiUtil.isTypeAi(source)) {
                message.setType(MessageTypeEnum.Ai);
            } else {
                message.setType(MessageTypeEnum.LangChainAi);
            }
            message.setText(cast.text());
            if (cast.hasToolExecutionRequests()) {
                message.setToolRequests(cast.toolExecutionRequests().stream()
                        .map(e -> {
                            ToolRequestVO toolRequest = new ToolRequestVO();
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
                        historyList = new ArrayList<>();
                    }
                }
            }
        }
        return historyList;
    }

    public List<ChatMessage> getRequestMessageList() {
        return requestTrace.getReadonlyMessageList().stream().map(MessageVO::getSource).collect(Collectors.toList());
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
        for (MessageVO<U> m : requestTrace.getReadonlyMessageList()) {
            messageList.add(m.getSource());
        }
        return messageList;
    }
}
