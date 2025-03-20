package com.github.aiassistant.service.text.chat;

import com.github.aiassistant.dao.AiChatClassifyMapper;
import com.github.aiassistant.entity.AiChatClassify;
import com.github.aiassistant.entity.model.chat.QuestionClassifyListVO;
import com.github.aiassistant.util.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * 增删改查-AI聊天分类
 */
public class AiChatClassifyServiceImpl {
    /**
     * 最大每秒1W的并发量
     */
    private static final int CONCURRENT_QPS = 10000 / 3;
    private static final Logger log = LoggerFactory.getLogger(AiChatClassifyServiceImpl.class);
    private final LinkedBlockingQueue<AiChatClassifyRequest> insertRequestQueue = new LinkedBlockingQueue<>(CONCURRENT_QPS);

    // @Resource
    private final AiChatClassifyMapper aiChatClassifyMapper;
    private int insertPartitionSize = 100;

    public AiChatClassifyServiceImpl(AiChatClassifyMapper aiChatClassifyMapper) {
        this.aiChatClassifyMapper = aiChatClassifyMapper;
        // 批量持久化（防止问答过猛）
        InsertBatchThread insertBatchThread = new InsertBatchThread();
        insertBatchThread.setName(getClass().getSimpleName() + "#insertBatch" + insertBatchThread.getId());
        insertBatchThread.start();
    }

    /**
     * 提交聊天分类
     */
    public CompletableFuture<AiChatClassifyRequest> insert(List<QuestionClassifyListVO.ClassifyVO> classifyResultList,
                                                           Integer chatId,
                                                           String question,
                                                           String userQueryTraceNumber) {
        Date now = new Date();
        try {
            AiChatClassifyRequest aiChatRequest = new AiChatClassifyRequest();
            for (QuestionClassifyListVO.ClassifyVO classifyVO : classifyResultList) {
                AiChatClassify insert = new AiChatClassify();
                insert.setCreateTime(now);
                insert.setAiChatId(chatId);
                insert.setUserQueryTraceNumber(Objects.toString(userQueryTraceNumber, ""));
                insert.setClassifyId(classifyVO.getId());
                insert.setClassifyName(Objects.toString(classifyVO.getClassifyName(), ""));
                insert.setClassifyGroupCode(Objects.toString(classifyVO.getGroupCode(), ""));
                insert.setClassifyGroupName(Objects.toString(classifyVO.getGroupName(), ""));
                insert.setQuestion(Objects.toString(question, ""));
                insert.setAiQuestionClassifyAssistantId(classifyVO.getAiQuestionClassifyAssistantId());
                aiChatRequest.classifyList.add(insert);
            }
            // 防止问答过猛
            while (!insertRequestQueue.offer(aiChatRequest)) {
                List<AiChatClassifyRequest> list = new ArrayList<>(insertRequestQueue.size());
                insertRequestQueue.drainTo(list);
                insert(list);
            }
            return aiChatRequest.future;
        } catch (Exception e) {
            CompletableFuture<AiChatClassifyRequest> future = new CompletableFuture<>();
            future.completeExceptionally(e);
            return future;
        }
    }

    /**
     * 持久化聊天分类
     */
    private void insert(List<AiChatClassifyRequest> insert) {
        try {
            List<AiChatClassify> insertList = new ArrayList<>();
            for (AiChatClassifyRequest request : insert) {
                insertList.addAll(request.classifyList);
            }
            Lists.partition(insertList, insertPartitionSize).forEach(aiChatClassifyMapper::insertBatchSomeColumn);
            for (AiChatClassifyRequest request : insert) {
                request.future.complete(request);
            }
        } catch (Exception e) {
            for (AiChatClassifyRequest request : insert) {
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
     * 聊天分类
     */
    // @Data
    public static class AiChatClassifyRequest {
        private final CompletableFuture<AiChatClassifyRequest> future = new CompletableFuture<>();
        private final List<AiChatClassify> classifyList = new ArrayList<>();
    }

    private class InsertBatchThread extends Thread {

        @Override
        public void run() {
            while (!isInterrupted()) {
                List<AiChatClassifyRequest> list = new ArrayList<>(Math.max(1, insertRequestQueue.size()));
                if (insertRequestQueue.isEmpty()) {
                    try {
                        list.add(insertRequestQueue.take());
                    } catch (InterruptedException e) {
                        log.info("AiChatClassifyServiceImpl InterruptedException {}", e.toString(), e);
                        return;
                    }
                } else {
                    insertRequestQueue.drainTo(list);
                }
                try {
                    insert(list);
                } catch (Exception e) {
                    log.error("AiChatClassifyServiceImpl insert request queue error {}", e.toString(), e);
                }
            }
        }
    }

}
