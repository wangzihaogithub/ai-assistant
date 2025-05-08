package com.github.aiassistant.service.text.memory;

import com.github.aiassistant.dao.AiMemoryMapper;
import com.github.aiassistant.dao.AiMemoryMessageKnMapper;
import com.github.aiassistant.dao.AiMemoryMessageMapper;
import com.github.aiassistant.dao.AiMemoryMessageToolMapper;
import com.github.aiassistant.entity.AiMemoryMessage;
import com.github.aiassistant.entity.AiMemoryMessageKn;
import com.github.aiassistant.entity.AiMemoryMessageTool;
import com.github.aiassistant.entity.model.chat.*;
import com.github.aiassistant.entity.model.user.AiAccessUserVO;
import com.github.aiassistant.serviceintercept.AiMemoryMessageServiceIntercept;
import com.github.aiassistant.util.AiUtil;
import com.github.aiassistant.util.Lists;
import com.github.aiassistant.util.StringUtils;
import dev.langchain4j.data.message.ChatMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * 增删改查-AI记忆的消息内容
 */
public class AiMemoryMessageServiceImpl {
    /**
     * 最大每秒1W的并发量
     */
    private static final int CONCURRENT_QPS = 10000 / 3;
    private static final Logger log = LoggerFactory.getLogger(AiMemoryMessageServiceImpl.class);
    private final LinkedBlockingQueue<AiMemoryVO> insertRequestQueue = new LinkedBlockingQueue<>(CONCURRENT_QPS);
    // @Resource
    private final AiMemoryMapper aiMemoryMapper;
    // @Resource
    private final AiMemoryMessageKnMapper aiMemoryMessageKnMapper;
    // @Resource
    private final AiMemoryMessageMapper aiMemoryMessageMapper;
    // @Resource
//    private final AiMemoryMessageToolJobMapper aiMemoryMessageToolJobMapper;
    // @Resource
    private final AiMemoryMessageToolMapper aiMemoryMessageToolMapper;
    private final Supplier<Collection<AiMemoryMessageServiceIntercept>> interceptList;
    private int insertPartitionSize = 100;

    public AiMemoryMessageServiceImpl(AiMemoryMapper aiMemoryMapper,
                                      AiMemoryMessageKnMapper aiMemoryMessageKnMapper,
                                      AiMemoryMessageMapper aiMemoryMessageMapper,
//                                      AiMemoryMessageToolJobMapper aiMemoryMessageToolJobMapper,
                                      AiMemoryMessageToolMapper aiMemoryMessageToolMapper,
                                      Supplier<Collection<AiMemoryMessageServiceIntercept>> interceptList) {
        this.aiMemoryMapper = aiMemoryMapper;
        this.aiMemoryMessageKnMapper = aiMemoryMessageKnMapper;
        this.aiMemoryMessageMapper = aiMemoryMessageMapper;
//        this.aiMemoryMessageToolJobMapper = aiMemoryMessageToolJobMapper;
        this.aiMemoryMessageToolMapper = aiMemoryMessageToolMapper;
        this.interceptList = interceptList;
        // 批量持久化（防止问答过猛）
        InsertBatchThread insertBatchThread = new InsertBatchThread();
        insertBatchThread.setName(getClass().getSimpleName() + "#insertBatch" + insertBatchThread.getId());
        insertBatchThread.start();
    }

    public int getInsertPartitionSize() {
        return insertPartitionSize;
    }

    public void setInsertPartitionSize(int insertPartitionSize) {
        this.insertPartitionSize = insertPartitionSize;
    }

    /**
     * 查询记忆
     *
     * @param memoryId                  memoryId
     * @param againUserQueryTraceNumber againUserQueryTraceNumber
     * @return 记忆
     */
    public List<ChatMessage> selectHistoryList(Integer memoryId, String againUserQueryTraceNumber) {
        String rootAgainUserQueryTraceNumber;
        if (StringUtils.hasText(againUserQueryTraceNumber)) {
            // 多次终止并重新回答，需要找到原始问题 rootAgainUserQueryTraceNumber
            rootAgainUserQueryTraceNumber = selectRootAgainUserQueryTraceNumberMap(Collections.singletonList(againUserQueryTraceNumber)).get(againUserQueryTraceNumber);
        } else {
            rootAgainUserQueryTraceNumber = null;
        }
        List<AiMemoryMessage> messageList = aiMemoryMessageMapper.selectListByMemoryId(memoryId, rootAgainUserQueryTraceNumber);
        return AiUtil.deserializeMemory(messageList);
    }

    /**
     * 提交记忆
     *
     * @param now                       now
     * @param requestTrace              requestTrace
     * @param againUserQueryTraceNumber againUserQueryTraceNumber
     * @param websearch                 websearch
     * @return 提交成功后
     */
    public CompletableFuture<AiMemoryVO> insert(Date now, RequestTraceVO<MemoryIdVO, AiAccessUserVO> requestTrace,
                                                String againUserQueryTraceNumber,
                                                Boolean websearch) {
        try {
            AiMemoryVO aiMemoryVO = buildMemory(now, requestTrace, againUserQueryTraceNumber, websearch);
            // 防止问答过猛
            while (!insertRequestQueue.offer(aiMemoryVO)) {
                List<AiMemoryVO> list = new ArrayList<>(insertRequestQueue.size());
                insertRequestQueue.drainTo(list);
                try {
                    insert(list);
                } catch (Exception e) {
                    log.error("AiMemoryMessageServiceImpl insert request queue error {}", e.toString(), e);
                }
            }
            return aiMemoryVO.future;
        } catch (Exception e) {
            CompletableFuture<AiMemoryVO> future = new CompletableFuture<>();
            future.completeExceptionally(e);
            return future;
        }
    }

    /**
     * 如果重新回答，将过去的回复删除
     *
     * @param messageVOList messageVOList
     */
    private void setDeleteTimeIfAgainQuery(List<AiMemoryMessageVO> messageVOList) {
        // 插入历史
        List<String> userQueryTraceNumberList = messageVOList.stream().map(AiMemoryMessageVO::getUserQueryTraceNumber).filter(StringUtils::hasText).collect(Collectors.toList());
        if (userQueryTraceNumberList.isEmpty()) {
            return;
        }
        // 入库前校验该记录是否被用户的后续操作给重新回答了。
        List<String> againUserQueryTraceNumberList = aiMemoryMessageMapper.selectAgainTraceNumberList(userQueryTraceNumberList);
        for (AiMemoryMessageVO vo : messageVOList) {
            // 解决用户操作时序问题：如果先停止之前的问题，后重新回答上个问题。这条记录就是被停止的上个问题。需要被删除，因为这条记录被重新回答了
            if (againUserQueryTraceNumberList.contains(vo.getUserQueryTraceNumber())) {
                vo.setDeleteTime(vo.getCreateTime());
            }
        }
    }

    /**
     * 查询原始问题编号（重新回答会复用）
     *
     * @param againUserQueryTraceNumberList againUserQueryTraceNumberList
     * @return 原始问题编号
     */
    public Map<String, String> selectRootAgainUserQueryTraceNumberMap(List<String> againUserQueryTraceNumberList) {
        return againUserQueryTraceNumberList.isEmpty() ? Collections.emptyMap() : aiMemoryMessageMapper.selectRootAgainTraceNumberList(againUserQueryTraceNumberList).stream()
                .collect(Collectors.toMap(AiMemoryMessage::getUserQueryTraceNumber, AiMemoryMessage::getRootUserQueryTraceNumber));
    }

    private void setRootUserQueryTraceNumberIfAgainQuery(List<AiMemoryMessageVO> historyList) {
        List<String> againUserQueryTraceNumberList = historyList.stream().map(AiMemoryMessageVO::getAgainUserQueryTraceNumber).filter(StringUtils::hasText).collect(Collectors.toList());
        // 冗余一下原始问题编号。方便查询
        Map<String, String> rootAgainUserQueryTraceNumberMap = selectRootAgainUserQueryTraceNumberMap(againUserQueryTraceNumberList);
        for (AiMemoryMessageVO vo : historyList) {
            // 冗余一下原始问题编号。方便查询
            vo.setRootUserQueryTraceNumber(Objects.toString(rootAgainUserQueryTraceNumberMap.get(vo.getAgainUserQueryTraceNumber()), ""));
        }
    }

    /**
     * 持久化记忆
     *
     * @param list list
     */
    public void insert(List<AiMemoryVO> list) {
        try {
            List<AiMemoryMessageVO> messageVOList = list.stream().flatMap(e -> e.getMessageList().stream()).collect(Collectors.toList());
            // 入库前校验该记录是否被用户的后续操作给重新回答了。
            setDeleteTimeIfAgainQuery(messageVOList);
            // 冗余一下原始问题编号。方便查询
            setRootUserQueryTraceNumberIfAgainQuery(messageVOList);

            Lists.partition(messageVOList, insertPartitionSize).forEach(aiMemoryMessageMapper::insertBatchSomeColumn);

            for (AiMemoryMessageVO messageVO : messageVOList) {
                Integer id = messageVO.getId();
                messageVO.getToolList().forEach(e -> e.setAiMemoryMessageId(id));
            }
            for (AiMemoryVO aiMemoryVO : list) {
                AiMemoryMessageVO user = aiMemoryVO.getMessageList().stream().filter(AiMemoryMessage::getUserQueryFlag).findFirst().orElse(null);
                if (user != null) {
                    for (AiMemoryMessageKn kn : aiMemoryVO.getKnList()) {
                        kn.setAiMemoryMessageId(user.getId());
                    }
                }
            }

            List<AiMemoryMessageTool> toolList = messageVOList.stream().flatMap(e -> e.getToolList().stream()).collect(Collectors.toList());
            List<AiMemoryMessageKn> knList = list.stream().flatMap(e -> e.getKnList().stream()).filter(e -> e.getAiMemoryMessageId() != null).collect(Collectors.toList());
            Lists.partition(toolList, insertPartitionSize).forEach(aiMemoryMessageToolMapper::insertBatchSomeColumn);
            Lists.partition(knList, insertPartitionSize).forEach(aiMemoryMessageKnMapper::insertBatchSomeColumn);

            for (AiMemoryMessageServiceIntercept intercept : interceptList.get()) {
                intercept.afterInsert(list);
            }
            for (AiMemoryVO aiMemoryVO : list) {
                aiMemoryMapper.updateTokens(aiMemoryVO.getId(),
                        aiMemoryVO.getUserTokenCount(), aiMemoryVO.getAiTokenCount(), aiMemoryVO.getKnowledgeTokenCount(),
                        aiMemoryVO.getUserCharLength(), aiMemoryVO.getAiCharLength(), aiMemoryVO.getKnowledgeCharLength(),
                        aiMemoryVO.getUpdateTime());

                aiMemoryVO.future.complete(aiMemoryVO);
            }
        } catch (Exception e) {
            for (AiMemoryVO aiMemoryVO : list) {
                aiMemoryVO.future.completeExceptionally(e);
            }
            throw e;
        }
    }

    private AiMemoryVO buildMemory(Date now, RequestTraceVO<MemoryIdVO, AiAccessUserVO> requestTrace,
                                   String againUserQueryTraceNumber,
                                   Boolean websearch) {
        Collection<AiMemoryMessageServiceIntercept> intercepts = interceptList.get();
        Integer memoryId = requestTrace.getMemoryId().getMemoryId();
        String userQueryTraceNumber = Objects.toString(requestTrace.getUserQueryTraceNumber(), "");

        AiMemoryVO aiMemory = new AiMemoryVO();
        aiMemory.setId(memoryId);
        aiMemory.setUpdateTime(now);

        List<MessageVO<AiAccessUserVO>> messageList;
        if (requestTrace.isStageRequest()) {
            messageList = requestTrace.getRequestMessageList();
            aiMemory.setUserCharLength(AiUtil.sumLength(messageList.stream().map(MessageVO::getSource).collect(Collectors.toList())));
            aiMemory.setAiCharLength(0);
        } else {
            messageList = requestTrace.getResponseMessageList();
            aiMemory.setAiCharLength(AiUtil.sumLength(messageList.stream().map(MessageVO::getSource).collect(Collectors.toList())));
            aiMemory.setUserCharLength(0);
        }
        aiMemory.setUserTokenCount(messageList.stream().mapToInt(MessageVO::getInputTokenCount).sum());
        aiMemory.setAiTokenCount(messageList.stream().mapToInt(MessageVO::getOutputTokenCount).sum());

        List<List<QaKnVO>> knowledgeList = requestTrace.getRequestKnowledgeList();
        aiMemory.setKnowledgeTokenCount(0);
        aiMemory.setKnowledgeCharLength(knowledgeList.stream().flatMap(Collection::stream).map(QaKnVO::getAnswer).mapToInt(String::length).sum());

        for (MessageVO<AiAccessUserVO> message : messageList) {
            Boolean userQueryFlag = message.getUserQueryFlag();
            ToolResponseVO toolResponse = message.getToolResponse();
            List<ChatMessage> sourceMessages = Collections.singletonList(message.getSource());
            Integer tokenCount = message.getTotalTokenCount();
            Integer inputTokenCount = message.getInputTokenCount();
            Integer outputTokenCount = message.getOutputTokenCount();

//            List<KnJobVO> jobList = message.getJobList();
            String memoryString = message.getMemoryString();
            if (memoryString == null || memoryString.isEmpty()) {
                memoryString = message.getText();
            }
            List<ToolRequestVO> toolRequests = message.getToolRequests();

            AiMemoryMessageVO vo = new AiMemoryMessageVO();
            vo.setAiMemoryId(memoryId);
            vo.setMessageIndex(message.getMessageIndex());
            vo.setMessageText(StringUtils.left(memoryString, 65000, true));
            vo.setMessageTypeEnum(message.getType().getCode());
            vo.setUserQueryFlag(userQueryFlag);
            vo.setCreateTime(message.getCreateTime());
            vo.setStartTime(message.getStartTime());
            vo.setFirstTokenTime(message.getFirstTokenTime());
            vo.setCommitTime(now);
            vo.setUseKnowledgeFlag(!knowledgeList.isEmpty());
            vo.setReplyToolRequestId(toolResponse != null ? toolResponse.getRequestId() : "");
            vo.setReplyToolName(toolResponse != null ? toolResponse.getToolName() : "");
            vo.setUseToolFlag(toolRequests != null && !toolRequests.isEmpty());
            vo.setKnowledgeTokenCount(0);
            vo.setTokenCount(tokenCount);
            vo.setUserTokenCount(inputTokenCount);
            vo.setAiTokenCount(outputTokenCount);
            vo.setKnowledgeCharLength(AiUtil.sumKnowledgeLength(sourceMessages));
            vo.setUserCharLength(AiUtil.sumUserLength(sourceMessages));
            vo.setAiCharLength(AiUtil.sumAiLength(sourceMessages));
            vo.setCharLength(AiUtil.sumLength(sourceMessages));
            vo.setUserQueryTraceNumber(userQueryTraceNumber);
            vo.setAgainUserQueryTraceNumber(Objects.toString(againUserQueryTraceNumber, ""));
            vo.setStageEnum(Objects.toString(requestTrace.getStageEnumKey(), ""));
            vo.setOpenAiRequestId(Objects.toString(message.getOpenAiRequestId(), ""));
            vo.setWebsearchFlag(websearch);

            if (toolRequests != null) {
                for (ToolRequestVO toolRequest : toolRequests) {
                    AiMemoryMessageTool toolVo = new AiMemoryMessageTool();
                    toolVo.setToolRequestId(toolRequest.getRequestId());
                    toolVo.setToolName(StringUtils.left(toolRequest.getToolName(), 128, true));
                    toolVo.setToolArguments(StringUtils.left(toolRequest.getArguments(), 65000, true));
                    toolVo.setAiMemoryId(memoryId);
                    vo.getToolList().add(toolVo);
                }
            }

            for (AiMemoryMessageServiceIntercept intercept : intercepts) {
                vo = intercept.afterMessage(vo, now, requestTrace, againUserQueryTraceNumber, websearch, message);
            }
            aiMemory.getMessageList().add(vo);
        }

        for (List<QaKnVO> qaKnVOS : knowledgeList) {
            for (QaKnVO qaKnVO : qaKnVOS) {
                AiMemoryMessageKn knVo = new AiMemoryMessageKn();
                knVo.setKnId(Integer.valueOf(qaKnVO.getId()));
                knVo.setKnQuestionText(StringUtils.left(qaKnVO.getQuestion(), 4000, true));
                knVo.setKnAnswerText(StringUtils.left(qaKnVO.getAnswer(), 4000, true));
                knVo.setKnScore(qaKnVO.scoreLong());
                knVo.setKnIndexUpdatedTime(qaKnVO.getIndexUpdatedTime());
                knVo.setKnIndexName(StringUtils.left(qaKnVO.getIndexName(), 128, true));
                knVo.setAiMemoryId(memoryId);
                aiMemory.getKnList().add(knVo);
            }
        }
        for (AiMemoryMessageServiceIntercept intercept : intercepts) {
            aiMemory = intercept.afterMemory(aiMemory, now, requestTrace, againUserQueryTraceNumber, websearch);
        }
        return aiMemory;
    }

    public static class AiMemoryVO {
        private final CompletableFuture<AiMemoryVO> future = new CompletableFuture<>();
        private Integer id;
        private Date updateTime;
        private Integer userTokenCount;
        private Integer aiTokenCount;
        private Integer knowledgeTokenCount;

        private Integer userCharLength;
        private Integer knowledgeCharLength;
        private Integer aiCharLength;

        private List<AiMemoryMessageKn> knList = new ArrayList<>();
        private List<AiMemoryMessageVO> messageList = new ArrayList<>();

        public CompletableFuture<AiMemoryVO> getFuture() {
            return future;
        }

        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
            this.id = id;
        }

        public Date getUpdateTime() {
            return updateTime;
        }

        public void setUpdateTime(Date updateTime) {
            this.updateTime = updateTime;
        }

        public Integer getUserTokenCount() {
            return userTokenCount;
        }

        public void setUserTokenCount(Integer userTokenCount) {
            this.userTokenCount = userTokenCount;
        }

        public Integer getAiTokenCount() {
            return aiTokenCount;
        }

        public void setAiTokenCount(Integer aiTokenCount) {
            this.aiTokenCount = aiTokenCount;
        }

        public Integer getKnowledgeTokenCount() {
            return knowledgeTokenCount;
        }

        public void setKnowledgeTokenCount(Integer knowledgeTokenCount) {
            this.knowledgeTokenCount = knowledgeTokenCount;
        }

        public Integer getUserCharLength() {
            return userCharLength;
        }

        public void setUserCharLength(Integer userCharLength) {
            this.userCharLength = userCharLength;
        }

        public Integer getKnowledgeCharLength() {
            return knowledgeCharLength;
        }

        public void setKnowledgeCharLength(Integer knowledgeCharLength) {
            this.knowledgeCharLength = knowledgeCharLength;
        }

        public Integer getAiCharLength() {
            return aiCharLength;
        }

        public void setAiCharLength(Integer aiCharLength) {
            this.aiCharLength = aiCharLength;
        }

        public List<AiMemoryMessageKn> getKnList() {
            return knList;
        }

        public void setKnList(List<AiMemoryMessageKn> knList) {
            this.knList = knList;
        }

        public List<AiMemoryMessageVO> getMessageList() {
            return messageList;
        }

        public void setMessageList(List<AiMemoryMessageVO> messageList) {
            this.messageList = messageList;
        }
    }

    public static class AiMemoryMessageVO extends AiMemoryMessage {
        private List<AiMemoryMessageTool> toolList = new ArrayList<>();

        public List<AiMemoryMessageTool> getToolList() {
            return toolList;
        }

        public void setToolList(List<AiMemoryMessageTool> toolList) {
            this.toolList = toolList;
        }
    }

    private class InsertBatchThread extends Thread {

        @Override
        public void run() {
            while (!isInterrupted()) {
                List<AiMemoryVO> list = new ArrayList<>(Math.max(1, insertRequestQueue.size()));
                if (insertRequestQueue.isEmpty()) {
                    try {
                        list.add(insertRequestQueue.take());
                    } catch (InterruptedException e) {
                        log.info("AiMemoryMessageServiceImpl InterruptedException {}", e.toString(), e);
                        return;
                    }
                }
                insertRequestQueue.drainTo(list);
                try {
                    insert(list);
                    if (log.isDebugEnabled()) {
                        log.debug("AiMemoryMessageServiceImpl insert request queue  {}", list.size());
                    }
                } catch (Exception e) {
                    log.error("AiMemoryMessageServiceImpl insert request queue error {}", e.toString(), e);
                }
            }
        }
    }
}
