package com.github.aiassistant.service.text.memory;

import com.github.aiassistant.dao.AiMemorySearchDocMapper;
import com.github.aiassistant.dao.AiMemorySearchMapper;
import com.github.aiassistant.entity.AiMemorySearch;
import com.github.aiassistant.entity.AiMemorySearchDoc;
import com.github.aiassistant.entity.model.chat.KnVO;
import com.github.aiassistant.service.text.embedding.KnnResponseListenerFuture;
import com.github.aiassistant.util.AiUtil;
import com.github.aiassistant.util.Lists;
import com.github.aiassistant.util.StringUtils;
import com.github.aiassistant.util.ThrowableUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

/**
 * 增删改查-记忆RAG
 */
//@Service
public class AiMemorySearchServiceImpl {
    /**
     * 最大每秒1W的并发量
     */
    private static final int CONCURRENT_QPS = 10000 / 3;
    private static final Logger log = LoggerFactory.getLogger(AiMemorySearchServiceImpl.class);
    private final LinkedBlockingQueue<AiMemorySearchRequest> insertRequestQueue = new LinkedBlockingQueue<>(CONCURRENT_QPS);

    // @Resource
    private final AiMemorySearchMapper aiMemorySearchMapper;
    private final AiMemorySearchDocMapper aiMemorySearchDocMapper;
    private int insertPartitionSize = 100;

    public AiMemorySearchServiceImpl(AiMemorySearchMapper aiMemorySearchMapper,
                                     AiMemorySearchDocMapper aiMemorySearchDocMapper) {
        this.aiMemorySearchMapper = aiMemorySearchMapper;
        this.aiMemorySearchDocMapper = aiMemorySearchDocMapper;
        // 批量持久化（防止问答过猛）
        InsertBatchThread insertBatchThread = new InsertBatchThread();
        insertBatchThread.setName(getClass().getSimpleName() + "#insertBatch" + insertBatchThread.getId());
        insertBatchThread.start();
    }

    /**
     * 提交RAG
     *
     * @param knnFuture            knnFuture
     * @param aiMemoryId           记忆ID
     * @param aiChatId             聊天ID
     * @param userQueryTraceNumber 提问
     * @return 提交RAG插入成功后
     */
    public CompletableFuture<AiMemorySearchRequest> insert(KnnResponseListenerFuture<? extends KnVO> knnFuture,
                                                           Integer aiMemoryId,
                                                           Integer aiChatId,
                                                           String userQueryTraceNumber) {
        AiMemorySearchRequest request = new AiMemorySearchRequest();
        try {
            knnFuture.whenComplete((knVOS, throwable) -> {
                Date searchEndTime = new Date();
                Date searchStartTime = new Date(knnFuture.getCreateTime());
                request.setAiMemoryId(aiMemoryId);
                request.setIndexName(knnFuture.getIndexName());
                request.setAiChatId(aiChatId);
                byte[] requestBodyBytes = knnFuture.getRequestBodyBytes();
                request.setRequestBody(requestBodyBytes == null ? "" : new String(requestBodyBytes, 0, Math.min(65500, requestBodyBytes.length)));
                request.setResponseDocCount(knVOS == null ? 0 : knVOS.size());
                request.setErrorMessage(throwable == null ? "" : StringUtils.left(ThrowableUtil.stackTraceToString(throwable), 3995, true));
                request.setUserQueryTraceNumber(StringUtils.left(userQueryTraceNumber, 32, true));
                request.setSearchStartTime(searchStartTime);
                request.setSearchEndTime(searchEndTime);
                request.setSearchCostMs((int) (searchEndTime.getTime() - searchStartTime.getTime()));
                if (knVOS != null) {
                    for (KnVO knVO : knVOS) {
                        AiMemorySearchDoc doc = new AiMemorySearchDoc();
                        doc.setDocIdString(StringUtils.left(knVO.getId(), 36, true));
                        String id = knVO.getId();
                        if (StringUtils.isPositiveNumeric(id)) {
                            try {
                                doc.setDocIdInt(Integer.valueOf(id));
                            } catch (Exception ignored) {
                            }
                        }
                        Long scored = AiUtil.scoreToLong(knVO.getScore());
                        doc.setDocScore(scored == null ? null : scored.intValue());
                        request.docList.add(doc);
                    }
                }
                // 防止问答过猛
                while (!insertRequestQueue.offer(request)) {
                    List<AiMemorySearchRequest> list = new ArrayList<>(insertRequestQueue.size());
                    insertRequestQueue.drainTo(list);
                    insert(list);
                }
            });
            return request.future;
        } catch (Exception e) {
            CompletableFuture<AiMemorySearchRequest> future = new CompletableFuture<>();
            future.completeExceptionally(e);
            return future;
        }
    }

    /**
     * 持久化RAG
     *
     * @param list list
     */
    private void insert(List<AiMemorySearchRequest> list) {
        try {
            Date now = new Date();
            list.forEach(e -> e.setCreateTime(now));
            Lists.partition(list, insertPartitionSize).forEach(aiMemorySearchMapper::insertBatchSomeColumn);

            list.forEach(e -> e.docList.forEach(e1 -> e1.setAiMemorySearchId(e.getId())));
            Collection<AiMemorySearchDoc> docList = list.stream().map(e -> e.docList).flatMap(Collection::stream).collect(Collectors.toList());
            Lists.partition(docList, insertPartitionSize).forEach(aiMemorySearchDocMapper::insertBatchSomeColumn);

            for (AiMemorySearchRequest request : list) {
                request.future.complete(request);
            }
        } catch (Exception e) {
            for (AiMemorySearchRequest request : list) {
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
     * RAG
     */
    // @Data
    public static class AiMemorySearchRequest extends AiMemorySearch {
        private final CompletableFuture<AiMemorySearchRequest> future = new CompletableFuture<>();
        private final List<AiMemorySearchDoc> docList = new ArrayList<>();
    }

    private class InsertBatchThread extends Thread {

        @Override
        public void run() {
            while (!isInterrupted()) {
                List<AiMemorySearchRequest> list = new ArrayList<>(Math.max(1, insertRequestQueue.size()));
                if (insertRequestQueue.isEmpty()) {
                    try {
                        list.add(insertRequestQueue.take());
                    } catch (InterruptedException e) {
                        log.info("AiMemorySearchServiceImpl InterruptedException {}", e.toString(), e);
                        return;
                    }
                }
                insertRequestQueue.drainTo(list);
                try {
                    insert(list);
                    if (log.isDebugEnabled()) {
                        log.debug("AiMemorySearchServiceImpl insert request queue  {}", list.size());
                    }
                } catch (Exception e) {
                    log.error("AiMemorySearchServiceImpl insert request queue error {}", e.toString(), e);
                }
            }
        }
    }

}
