package com.github.aiassistant.service.text.chat;

import com.github.aiassistant.dao.*;
import com.github.aiassistant.entity.AiChatAbort;
import com.github.aiassistant.entity.AiChatHistory;
import com.github.aiassistant.entity.AiMemoryError;
import com.github.aiassistant.entity.model.chat.*;
import com.github.aiassistant.entity.model.user.AiAccessUserVO;
import com.github.aiassistant.enums.AiChatUidTypeEnum;
import com.github.aiassistant.enums.MessageTypeEnum;
import com.github.aiassistant.serviceintercept.AiChatHistoryServiceIntercept;
import com.github.aiassistant.util.Lists;
import com.github.aiassistant.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * 增删改查-用户和AI聊天的历史记录
 */
public class AiChatHistoryServiceImpl {
    /**
     * 最大每秒1W的并发量
     */
    private static final int CONCURRENT_QPS = 10000 / 3;
    private static final Logger log = LoggerFactory.getLogger(AiChatHistoryServiceImpl.class);
    private final LinkedBlockingQueue<AiChatRequest> insertRequestQueue = new LinkedBlockingQueue<>(CONCURRENT_QPS);

    // @Resource
    private final AiMemoryMessageMapper aiMemoryMessageMapper;
    // @Resource
    private final AiChatHistoryMapper aiChatHistoryMapper;
    // @Resource
//    private final AiChatHistoryJobMapper aiChatHistoryJobMapper;
    // @Resource
    private final AiMemoryErrorMapper aiMemoryErrorMapper;
    // @Resource
    private final AiChatWebsearchMapper aiChatWebsearchMapper;
    // @Resource
    private final AiChatMapper aiChatMapper;
    // @Resource
    private final AiChatAbortMapper aiChatAbortMapper;
    private final Supplier<Collection<AiChatHistoryServiceIntercept>> interceptList;
    private int insertPartitionSize = 100;

    public AiChatHistoryServiceImpl(AiMemoryMessageMapper aiMemoryMessageMapper,
                                    AiChatHistoryMapper aiChatHistoryMapper,
//                                    AiChatHistoryJobMapper aiChatHistoryJobMapper,
                                    AiMemoryErrorMapper aiMemoryErrorMapper,
                                    AiChatWebsearchMapper aiChatWebsearchMapper,
                                    AiChatMapper aiChatMapper,
                                    AiChatAbortMapper aiChatAbortMapper,
                                    Supplier<Collection<AiChatHistoryServiceIntercept>> interceptList) {
        this.aiMemoryMessageMapper = aiMemoryMessageMapper;
        this.aiChatHistoryMapper = aiChatHistoryMapper;
//        this.aiChatHistoryJobMapper = aiChatHistoryJobMapper;
        this.aiMemoryErrorMapper = aiMemoryErrorMapper;
        this.aiChatWebsearchMapper = aiChatWebsearchMapper;
        this.aiChatMapper = aiChatMapper;
        this.interceptList = interceptList;
        this.aiChatAbortMapper = aiChatAbortMapper;
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
     * 是否还有今日可用字数
     *
     * @param createUid     createUid
     * @param uidTypeEnum   uidTypeEnum
     * @param maxTokenCount maxTokenCount
     * @return 是否还有今日可用字数
     */
    public boolean hasTokens(Serializable createUid, AiChatUidTypeEnum uidTypeEnum, int maxTokenCount) {
        return sumTodayCharLength(createUid, uidTypeEnum).isHasTokens(maxTokenCount);
    }

    /**
     * 统计今日字数
     *
     * @param createUid   createUid
     * @param uidTypeEnum uidTypeEnum
     * @return 统计今日字数
     */
    public AiChatToken sumTodayCharLength(Serializable createUid, AiChatUidTypeEnum uidTypeEnum) {
        int tokenCount = aiChatHistoryMapper.sumTodayCharLength(uidTypeEnum.getCode(), createUid);
        AiChatToken token = new AiChatToken();
        token.setTokenCount(tokenCount);
        return token;
    }

    /**
     * 查询最后一次聊天号
     *
     * @param chatId chatId
     * @return 最后一次聊天号
     */
    public String selectLastUserAgainTraceNumber(Integer chatId) {
        return aiChatHistoryMapper.selectLastUserAgainTraceNumber(chatId);
    }

    /**
     * 查询聊天记录
     *
     * @param aiChatId aiChatId
     * @return 聊天记录
     */
    public List<AiUserChatHistoryResp> selectListByChatId(Integer aiChatId) {
        // 查询聊天记录
        List<AiChatHistoryResp> list = aiChatHistoryMapper.selectListByChatId(aiChatId);
        if (list.isEmpty()) {
            return Collections.emptyList();
        }
        // 联网
        List<AiChatWebsearchResp> websearchList = aiChatWebsearchMapper.selectListByChatId(aiChatId);
        // 查询终止回复
        List<AiChatAbort> abortList = aiChatAbortMapper.selectListByChatId(aiChatId);
        // 查询错误
        List<AiMemoryError> memoryErrorList = aiMemoryErrorMapper.selectListByChatId(aiChatId);

        // 查询岗位
//        List<AiChatHistoryJobResp> knJobList = aiChatHistoryJobMapper.selectListByHistoryId(CollUtil.map(list, AiChatHistoryResp::getId, true));
//        Map<Integer, List<AiChatHistoryJobResp>> jobListMap = knJobList.stream().collect(Collectors.groupingBy(AiChatHistoryJobResp::getAiChatHistoryId));
//        List<Integer> jobIdList = CollUtil.map(knJobList, e -> Integer.valueOf(e.getId()), true);

        // 转换岗位
//        Map<String, AiJobSearchVO> jobModelMap = jobSearchAdapter.convertJob(jobIdList);
//        for (AiChatHistoryResp row : list) {
//            List<AiChatHistoryJobResp> rowKnJobList = jobListMap.get(row.getId());
//            if (rowKnJobList != null) {
//                row.setJobList(rowKnJobList.stream()
//                        .map(e -> KnEsJob.convert((AiChatHistoryJobResp) null, jobModelMap.get(e.getId())))
//                        .collect(Collectors.toList()));
//            }
//        }

        // post before intercepts
        Collection<AiChatHistoryServiceIntercept> intercepts = interceptList.get();
        for (AiChatHistoryServiceIntercept intercept : intercepts) {
            list = intercept.beforeSelectListByChatId(aiChatId, list, abortList, memoryErrorList, websearchList);
        }

        // 根据用户问题分组
        List<AiUserChatHistoryResp> result = AiUserChatHistoryResp.groupByUser(list, abortList, memoryErrorList, websearchList);

        // post after intercepts
        for (AiChatHistoryServiceIntercept intercept : intercepts) {
            result = intercept.afterSelectListByChatId(aiChatId, result, list, abortList, memoryErrorList, websearchList);
        }
        return result;
    }

    /**
     * 提交聊天记录
     *
     * @param now                       now
     * @param requestTrace              requestTrace
     * @param againUserQueryTraceNumber againUserQueryTraceNumber
     * @param websearch                 websearch
     * @param user                      user
     * @return 提交结果
     */
    public CompletableFuture<AiChatRequest> insert(Date now, RequestTrace<MemoryIdVO, AiAccessUserVO> requestTrace,
                                                   String againUserQueryTraceNumber,
                                                   Boolean websearch,
                                                   CompletableFuture<AiChatRequest> user) {
        try {
            // 提交聊天记录
            AiChatRequest aiChatRequest = buildAiChatVO(now, requestTrace, againUserQueryTraceNumber, websearch, user);
            aiChatRequest.setAgainUserQueryTraceNumber(againUserQueryTraceNumber);
            // 防止问答过猛
            while (!insertRequestQueue.offer(aiChatRequest)) {
                List<AiChatRequest> list = new ArrayList<>(insertRequestQueue.size());
                insertRequestQueue.drainTo(list);
                insert(list);
            }
            return aiChatRequest.future;
        } catch (Exception e) {
            CompletableFuture<AiChatRequest> future = new CompletableFuture<>();
            future.completeExceptionally(e);
            return future;
        }
    }

    /**
     * 持久化重新回答
     *
     * @param againVOList againVOList
     */
    private void insertAgain(List<AiChatRequest> againVOList) {
        List<String> againUserQueryTraceNumberList = againVOList.stream().map(AiChatRequest::getAgainUserQueryTraceNumber).filter(StringUtils::hasText).collect(Collectors.toList());
        if (againUserQueryTraceNumberList.isEmpty()) {
            return;
        }
        // 修改历史记录
        Date lastChatTime = againVOList.stream().map(AiChatRequest::getLastChatTime).max(Date::compareTo).orElse(null);
        aiChatHistoryMapper.updateDeleteByUserQueryTraceNumber(lastChatTime, againUserQueryTraceNumberList);

        // 修改记忆
        aiMemoryMessageMapper.updateDeleteByUserQueryTraceNumber(lastChatTime, againUserQueryTraceNumberList);
    }

    /**
     * 持久化聊天记录
     *
     * @param list list
     */
    public void insert(List<AiChatRequest> list) {
        try {
            // 持久化重新回答
            insertAgain(list);
            // 持久化聊天记录
            insertHistory(list);

            // 更新最近聊天时间
            Date lastChatTime = list.stream().map(AiChatRequest::getLastChatTime).max(Date::compareTo).orElse(null);
            List<Integer> chatIdList = list.stream().map(AiChatRequest::getId).collect(Collectors.toList());
            aiChatMapper.updateLastChatTime(chatIdList, lastChatTime);

            // 更新最近联网状态
            Map<Boolean, List<AiChatRequest>> lastWebsearchMap = list.stream().collect(Collectors.groupingBy(e -> Boolean.TRUE.equals(e.getLastWebsearchFlag())));
            for (Map.Entry<Boolean, List<AiChatRequest>> entry : lastWebsearchMap.entrySet()) {
                List<Integer> websearchChatIdList = entry.getValue().stream().map(AiChatRequest::getId).collect(Collectors.toList());
                aiChatMapper.updateLastWebsearchFlag(websearchChatIdList, entry.getKey());
            }
            for (AiChatRequest request : list) {
                request.future.complete(request);
            }
        } catch (Exception e) {
            for (AiChatRequest request : list) {
                request.future.completeExceptionally(e);
            }
            throw e;
        }
    }

    private void setDeleteTimeIfAgainQuery(List<AiChatHistoryVO> historyList) {
        // 插入历史
        List<String> userQueryTraceNumberList = historyList.stream().map(AiChatHistory::getUserQueryTraceNumber).filter(StringUtils::hasText).collect(Collectors.toList());
        if (userQueryTraceNumberList.isEmpty()) {
            return;
        }
        // 入库前校验该记录是否被用户的后续操作给重新回答了。
        List<String> againUserQueryTraceNumberList = aiChatHistoryMapper.selectAgainTraceNumberList(userQueryTraceNumberList);
        for (AiChatHistoryVO vo : historyList) {
            // 解决用户操作时序问题：如果先停止之前的问题，后重新回答上个问题。这条记录就是被停止的上个问题。需要被删除，因为这条记录被重新回答了
            if (againUserQueryTraceNumberList.contains(vo.getUserQueryTraceNumber())) {
                vo.setDeleteTime(vo.getCreateTime());
            }
        }
    }

    public Map<String, String> selectRootAgainUserQueryTraceNumberMap(List<String> againUserQueryTraceNumberList) {
        return againUserQueryTraceNumberList.isEmpty() ? Collections.emptyMap() : aiChatHistoryMapper.selectRootAgainTraceNumberList(againUserQueryTraceNumberList).stream()
                .collect(Collectors.toMap(AiChatHistory::getUserQueryTraceNumber, AiChatHistory::getRootUserQueryTraceNumber));
    }

    private void setRootUserQueryTraceNumberIfAgainQuery(List<AiChatHistoryVO> historyList) {
        List<String> againUserQueryTraceNumberList = historyList.stream().map(AiChatHistory::getAgainUserQueryTraceNumber).filter(StringUtils::hasText).collect(Collectors.toList());
        // 冗余一下原始问题编号。方便查询
        Map<String, String> rootAgainUserQueryTraceNumberMap = selectRootAgainUserQueryTraceNumberMap(againUserQueryTraceNumberList);
        for (AiChatHistoryVO vo : historyList) {
            // 冗余一下原始问题编号。方便查询
            vo.setRootUserQueryTraceNumber(Objects.toString(rootAgainUserQueryTraceNumberMap.get(vo.getAgainUserQueryTraceNumber()), ""));
        }
    }

    /**
     * 持久化聊天记录
     *
     * @param list list
     */
    private void insertHistory(List<AiChatRequest> list) {
        List<AiChatHistoryVO> historyList = list.stream().flatMap(e -> e.getHistoryList().stream()).collect(Collectors.toList());
        // 入库前校验该记录是否被用户的后续操作给重新回答了。
        setDeleteTimeIfAgainQuery(historyList);
        // 冗余一下原始问题编号。方便查询
        setRootUserQueryTraceNumberIfAgainQuery(historyList);
        Lists.partition(historyList, insertPartitionSize).forEach(aiChatHistoryMapper::insertBatchSomeColumn);

        // 回填终止的父ID
        Map<String, AiChatHistoryVO> update = historyList.stream().collect(Collectors.toMap(AiChatHistory::getUserQueryTraceNumber, e -> e, (o1, o2) -> o1));
        for (Map.Entry<String, AiChatHistoryVO> entry : update.entrySet()) {
            String rootUserQueryTraceNumber = entry.getValue().getRootUserQueryTraceNumber();
            if (StringUtils.hasText(rootUserQueryTraceNumber)) {
                aiChatAbortMapper.updateRootAgainUserQueryTraceNumber(entry.getKey(), rootUserQueryTraceNumber);
            }
        }

        // 回填用户和AI的聊天关系
        updateUserChatHistoryId(list);

        for (AiChatHistoryServiceIntercept intercept : interceptList.get()) {
            intercept.afterInsert(list);
        }
    }

    /**
     * 回填用户和AI的聊天关系
     *
     * @param list list
     */
    private void updateUserChatHistoryId(List<AiChatRequest> list) {
        for (AiChatRequest request : list) {
            List<Integer> historyIdList = request.getHistoryList().stream().map(AiChatHistory::getId).collect(Collectors.toList());
            if (historyIdList.isEmpty()) {
                continue;
            }
            request.getUserChatHistoryId(aiChatHistoryMapper).thenAccept(userChatId -> {
                        // 回填用户提问的聊天ID
                        aiChatHistoryMapper.updateUserChatHistoryIdByIds(historyIdList, userChatId);
                    })
                    .exceptionally(throwable -> {
                        log.error("getUserQueryId  error. id = {}, error {}", request.getId(), throwable.toString(), throwable);
                        return null;
                    });
        }
    }

    protected AiChatHistoryVO afterMessage(AiChatHistoryVO source, Date now, RequestTrace<MemoryIdVO, AiAccessUserVO> requestTrace,
                                           String againUserQueryTraceNumber, Boolean websearch,
                                           CompletableFuture<AiChatRequest> user, Message<AiAccessUserVO> message) {
        return source;
    }

    protected AiChatRequest afterChat(AiChatRequest source, Date now, RequestTrace<MemoryIdVO, AiAccessUserVO> requestTrace,
                                      String againUserQueryTraceNumber, Boolean websearch,
                                      CompletableFuture<AiChatRequest> user) {
        return source;
    }

    protected void afterInsertHistory(List<AiChatRequest> list) {
        // 插入推荐岗位
//        for (AiChatHistoryVO historyVO : historyList) {
//            // 回填历史ID
//            historyVO.getJobList().forEach(e -> e.setAiChatHistoryId(historyVO.getId()));
//        }
//        aiChatHistoryJobMapper.insertBatchSomeColumnDefault(historyList.stream().flatMap(e -> e.getJobList().stream()).collect(Collectors.toList()));

    }

    private AiChatRequest buildAiChatVO(Date now, RequestTrace<MemoryIdVO, AiAccessUserVO> requestTrace,
                                        String againUserQueryTraceNumber, Boolean websearch,
                                        CompletableFuture<AiChatRequest> user) {
        Integer chatId = requestTrace.getMemoryId().getChatId();
        String userQueryTraceNumber = Objects.toString(requestTrace.getUserQueryTraceNumber(), "");

        AiChatRequest chatVO = new AiChatRequest();
        chatVO.setId(chatId);
        chatVO.setLastChatTime(now);
        chatVO.setLastWebsearchFlag(websearch);
        chatVO.setUser(user);

        Collection<AiChatHistoryServiceIntercept> intercepts = interceptList.get();
        List<Message<AiAccessUserVO>> messageList = requestTrace.isStageRequest() ? requestTrace.getRequestMessageList() : requestTrace.getResponseMessageList();
        for (Message<AiAccessUserVO> message : messageList) {
            if (!MessageTypeEnum.isChatType(message.getType())) {
                continue;
            }
            String text = MessageTypeEnum.isToolResult(message.getType()) ?
                    "" : message.getText();
            AiChatHistoryVO vo = new AiChatHistoryVO();
            vo.setAiChatId(chatId);
            vo.setUserQueryFlag(message.getUserQueryFlag());
            vo.setUserQueryTraceNumber(userQueryTraceNumber);
            vo.setCreateTime(message.getCreateTime());
            vo.setStartTime(message.getStartTime());
            vo.setMessageTypeEnum(Objects.toString(message.getType().getCode(), ""));
            vo.setMessageText(StringUtils.left(text, 65000, true));
            vo.setTextCharLength(Objects.toString(text, "").length());
            vo.setMessageIndex(message.getMessageIndex());
            vo.setAgainUserQueryTraceNumber(Objects.toString(againUserQueryTraceNumber, ""));
            vo.setStageEnum(Objects.toString(requestTrace.getStageEnumKey(), ""));
            vo.setWebsearchFlag(websearch);
//            List<KnJobVO> jobList = message.getJobList();
//            if (jobList != null) {
//                int index = 0;
//                for (KnJobVO knJobVO : jobList) {
//                    if (StringUtils.hasText(knJobVO.getId())) {
//                        AiChatHistoryJob historyJob = new AiChatHistoryJob();
//                        historyJob.setAiChatId(chatId);
//                        historyJob.setJobId(Integer.valueOf(knJobVO.getId()));
//                        historyJob.setJobName(StringUtils.limit(knJobVO.getName(), 512, true));
//                        historyJob.setJobIndex(index);
//                        historyJob.setJobScore(knJobVO.scoreLong());
//                        historyJob.setJobIndexUpdatedTime(knJobVO.getIndexUpdatedTime());
//                        historyJob.setJobIndexName(StringUtils.limit(knJobVO.getIndexName(), 128, true));
//                        vo.getJobList().add(historyJob);
//                    }
//                    index++;
//                }
//            }

            for (AiChatHistoryServiceIntercept intercept : intercepts) {
                vo = intercept.afterMessage(vo, now, requestTrace, againUserQueryTraceNumber, websearch, user, message);
            }
            chatVO.getHistoryList().add(vo);
        }
        for (AiChatHistoryServiceIntercept intercept : intercepts) {
            chatVO = intercept.afterChat(chatVO, now, requestTrace, againUserQueryTraceNumber, websearch, user);
        }
        return chatVO;
    }

    /**
     * 聊天记录
     */
    // @Data
    public static class AiChatRequest {
        private final CompletableFuture<AiChatRequest> future = new CompletableFuture<>();
        private CompletableFuture<AiChatRequest> user;
        private Integer id;
        private Date lastChatTime;
        private Boolean lastWebsearchFlag;
        private List<AiChatHistoryVO> historyList = new ArrayList<>();
        /**
         * 重新回答
         */
        private String againUserQueryTraceNumber;

        private Integer userChatHistoryId;

        public CompletableFuture<AiChatRequest> getFuture() {
            return future;
        }

        public CompletableFuture<AiChatRequest> getUser() {
            return user;
        }

        public void setUser(CompletableFuture<AiChatRequest> user) {
            this.user = user;
        }

        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
            this.id = id;
        }

        public Date getLastChatTime() {
            return lastChatTime;
        }

        public void setLastChatTime(Date lastChatTime) {
            this.lastChatTime = lastChatTime;
        }

        public Boolean getLastWebsearchFlag() {
            return lastWebsearchFlag;
        }

        public void setLastWebsearchFlag(Boolean lastWebsearchFlag) {
            this.lastWebsearchFlag = lastWebsearchFlag;
        }

        public List<AiChatHistoryVO> getHistoryList() {
            return historyList;
        }

        public void setHistoryList(List<AiChatHistoryVO> historyList) {
            this.historyList = historyList;
        }

        public String getAgainUserQueryTraceNumber() {
            return againUserQueryTraceNumber;
        }

        public void setAgainUserQueryTraceNumber(String againUserQueryTraceNumber) {
            this.againUserQueryTraceNumber = againUserQueryTraceNumber;
        }

        public Integer getUserChatHistoryId() {
            return userChatHistoryId;
        }

        public void setUserChatHistoryId(Integer userChatHistoryId) {
            this.userChatHistoryId = userChatHistoryId;
        }

        /**
         * 获取这次用户的问题ID
         *
         * @param aiChatHistoryMapper aiChatHistoryMapper
         * @return 这次用户的问题ID
         */
        public CompletableFuture<Integer> getUserChatHistoryId(AiChatHistoryMapper aiChatHistoryMapper) {
            if (userChatHistoryId != null) {
                return CompletableFuture.completedFuture(userChatHistoryId);
            }
            CompletableFuture<AiChatRequest> user = this.user;
            if (user == null) {
                user = CompletableFuture.completedFuture(this);
            }
            CompletableFuture<Integer> future1 = user.thenApply(e -> e.getHistoryList().stream().
                    filter(AiChatHistory::getUserQueryFlag)
                    .max(Comparator.comparing(AiChatHistory::getMessageIndex).thenComparing(AiChatHistory::getId))
                    .map(AiChatHistory::getId)
                    // 如果是重新回答取最后一次的问题ID
                    .orElseGet(() -> aiChatHistoryMapper.selectLastUserChatHistoryId(id, againUserQueryTraceNumber)));
            return future1.thenApply(id -> userChatHistoryId = id);
        }
    }

    // @Data
    public static class AiChatHistoryVO extends AiChatHistory {
//        private List<AiChatHistoryJob> jobList = new ArrayList<>();
    }

    private class InsertBatchThread extends Thread {

        @Override
        public void run() {
            while (!isInterrupted()) {
                List<AiChatRequest> list = new ArrayList<>(Math.max(1, insertRequestQueue.size()));
                if (insertRequestQueue.isEmpty()) {
                    try {
                        list.add(insertRequestQueue.take());
                    } catch (InterruptedException e) {
                        log.info("AiChatHistoryServiceImpl InterruptedException {}", e.toString(), e);
                        return;
                    }
                }
                insertRequestQueue.drainTo(list);
                try {
                    insert(list);
                    if (log.isDebugEnabled()) {
                        log.debug("AiChatHistoryServiceImpl insert request queue  {}", list.size());
                    }
                } catch (Exception e) {
                    log.error("AiChatHistoryServiceImpl insert request queue error {}", e.toString(), e);
                }
            }
        }
    }

}
