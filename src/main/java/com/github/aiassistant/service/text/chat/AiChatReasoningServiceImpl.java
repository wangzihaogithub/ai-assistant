package com.github.aiassistant.service.text.chat;

import com.github.aiassistant.dao.AiChatHistoryMapper;
import com.github.aiassistant.dao.AiChatReasoningMapper;
import com.github.aiassistant.dao.AiChatReasoningPlanMapper;
import com.github.aiassistant.entity.AiChatReasoning;
import com.github.aiassistant.entity.AiChatReasoningPlan;
import com.github.aiassistant.platform.JsonUtil;
import com.github.aiassistant.service.jsonschema.ReasoningJsonSchema;
import com.github.aiassistant.service.text.acting.ActingService;
import com.github.aiassistant.util.AiUtil;
import com.github.aiassistant.util.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

/**
 * 增删改查-AI思考
 */
//@Service
public class AiChatReasoningServiceImpl {
    /**
     * 最大每秒1W的并发量
     */
    private static final int CONCURRENT_QPS = 10000 / 3;
    private static final Logger log = LoggerFactory.getLogger(AiChatReasoningServiceImpl.class);
    private final LinkedBlockingQueue<AiChatReasoningRequest> insertRequestQueue = new LinkedBlockingQueue<>(CONCURRENT_QPS);

    // @Resource
    private final AiChatReasoningMapper aiChatReasoningMapper;
    // @Resource
    private final AiChatReasoningPlanMapper aiChatReasoningPlanMapper;
    // @Resource
    private final AiChatHistoryMapper aiChatHistoryMapper;
    private int insertPartitionSize = 100;

    public AiChatReasoningServiceImpl(AiChatReasoningMapper aiChatReasoningMapper,
                                      AiChatReasoningPlanMapper aiChatReasoningPlanMapper,
                                      AiChatHistoryMapper aiChatHistoryMapper) {
        this.aiChatReasoningMapper = aiChatReasoningMapper;
        this.aiChatReasoningPlanMapper = aiChatReasoningPlanMapper;
        this.aiChatHistoryMapper = aiChatHistoryMapper;
        // 批量持久化（防止问答过猛）
        InsertBatchThread insertBatchThread = new InsertBatchThread();
        insertBatchThread.setName(getClass().getSimpleName() + "#insertBatch" + insertBatchThread.getId());
        insertBatchThread.start();
    }

    /**
     * 提交思考
     */
    public CompletableFuture<AiChatReasoningRequest> insert(String question,
                                                            ActingService.Plan plan,
                                                            ReasoningJsonSchema.Result reason,
                                                            String userQueryTraceNumber,
                                                            CompletableFuture<AiChatHistoryServiceImpl.AiChatRequest> user) {
        try {
            // 提交思考
            AiChatReasoningRequest aiChatRequest = new AiChatReasoningRequest(question, plan, reason, user);
            aiChatRequest.setCreateTime(new Date());
            aiChatRequest.setUserQueryTraceNumber(userQueryTraceNumber);
            // 防止问答过猛
            while (!insertRequestQueue.offer(aiChatRequest)) {
                List<AiChatReasoningRequest> list = new ArrayList<>(insertRequestQueue.size());
                insertRequestQueue.drainTo(list);
                insert(list);
            }
            return aiChatRequest.future;
        } catch (Exception e) {
            CompletableFuture<AiChatReasoningRequest> future = new CompletableFuture<>();
            future.completeExceptionally(e);
            return future;
        }
    }

    /**
     * 持久化思考
     */
    private void insert(List<AiChatReasoningRequest> list) {
        JsonUtil.ObjectWriter objectWriter = JsonUtil.objectWriter();
        Collection<AiChatReasoningRequest> errorList = Collections.newSetFromMap(new IdentityHashMap<>());
        for (AiChatReasoningRequest request : list) {
            try {
                AiChatHistoryServiceImpl.AiChatRequest user = request.user.get();
                Integer userChatHistoryId = user.getUserChatHistoryId(aiChatHistoryMapper).get();

                request.setAiChatId(user.getId());
                request.setQuestion(AiUtil.limit(request.question, 3950, true));
                request.setNeedSplittingFlag(request.reason.needSplitting);
                request.setUserChatHistoryId(userChatHistoryId);
                if (request.plan != null) {
                    request.setCreateTime(request.plan.getCreateTime());
                }
                for (ActingService.Plan plan = request.plan; plan != null; plan = plan.getNext()) {
                    AiChatReasoningPlan insertPlan = new AiChatReasoningPlan();
                    insertPlan.setAiChatId(request.getAiChatId());
                    insertPlan.setUserChatHistoryId(request.getUserChatHistoryId());
                    insertPlan.setTask(AiUtil.limit(plan.getTask(), 512, true));
                    insertPlan.setFailMessage(AiUtil.limit(plan.getResult().failMessage, 2000, true));
                    insertPlan.setAnswer(AiUtil.limit(plan.getResult().answer, 65000, true));
                    insertPlan.setAiQuestion(AiUtil.limit(plan.getResult().aiQuestion, 512, true));
                    Collection<String> websearchKeyword = plan.getResult().websearchKeyword;
                    insertPlan.setWebsearchKeyword(websearchKeyword == null || websearchKeyword.isEmpty() ? "" : AiUtil.limit(objectWriter.writeValueAsString(websearchKeyword), 1024, true));
                    insertPlan.setResolvedFlag(plan.getResult().resolved);
                    insertPlan.setPlanIndex(plan.getIndex());

                    request.planList.add(insertPlan);
                }
            } catch (Exception e) {
                request.future.completeExceptionally(e);
                errorList.add(request);
            }
        }
        List<AiChatReasoningRequest> insert = list.stream().filter(e -> !errorList.contains(e)).collect(Collectors.toList());
        try {
            Lists.partition(insert, insertPartitionSize).forEach(aiChatReasoningMapper::insertBatchSomeColumn);

            insert.forEach(e -> e.planList.forEach(e1 -> e1.setAiChatReasoningId(e.getId())));
            Collection<AiChatReasoningPlan> planList = insert.stream().map(e -> e.planList).flatMap(Collection::stream).collect(Collectors.toList());
            Lists.partition(planList, insertPartitionSize).forEach(aiChatReasoningPlanMapper::insertBatchSomeColumn);

            for (AiChatReasoningRequest request : insert) {
                request.future.complete(request);
            }
        } catch (Exception e) {
            for (AiChatReasoningRequest request : insert) {
                request.future.completeExceptionally(e);
            }
            throw e;
        }
    }

    public int getInsertPartitionSize() {
        return insertPartitionSize;
    }

    public void setInsertPartitionSize(int insertPartitionSize) {
        this.insertPartitionSize = insertPartitionSize;
    }

    /**
     * 思考
     */
    // @Data
    public static class AiChatReasoningRequest extends AiChatReasoning {
        private final CompletableFuture<AiChatReasoningRequest> future = new CompletableFuture<>();
        private final String question;
        private final ActingService.Plan plan;
        private final ReasoningJsonSchema.Result reason;
        private final CompletableFuture<AiChatHistoryServiceImpl.AiChatRequest> user;
        private final List<AiChatReasoningPlan> planList = new ArrayList<>();

        public AiChatReasoningRequest(String question, ActingService.Plan plan, ReasoningJsonSchema.Result reason, CompletableFuture<AiChatHistoryServiceImpl.AiChatRequest> user) {
            this.question = question;
            this.plan = plan;
            this.reason = reason;
            this.user = user;
        }
    }

    private class InsertBatchThread extends Thread {

        @Override
        public void run() {
            while (!isInterrupted()) {
                List<AiChatReasoningRequest> list = new ArrayList<>(Math.max(1, insertRequestQueue.size()));
                if (insertRequestQueue.isEmpty()) {
                    try {
                        list.add(insertRequestQueue.take());
                    } catch (InterruptedException e) {
                        log.info("AiChatReasoningServiceImpl InterruptedException {}", e.toString(), e);
                        return;
                    }
                } else {
                    insertRequestQueue.drainTo(list);
                }
                try {
                    insert(list);
                } catch (Exception e) {
                    log.error("AiChatReasoningServiceImpl insert request queue error {}", e.toString(), e);
                }
            }
        }
    }

}
