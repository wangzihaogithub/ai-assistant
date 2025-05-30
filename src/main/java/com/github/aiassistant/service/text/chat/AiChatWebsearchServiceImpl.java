package com.github.aiassistant.service.text.chat;

import com.github.aiassistant.dao.AiChatHistoryMapper;
import com.github.aiassistant.dao.AiChatWebsearchMapper;
import com.github.aiassistant.dao.AiChatWebsearchResultMapper;
import com.github.aiassistant.entity.AiChatWebsearch;
import com.github.aiassistant.entity.AiChatWebsearchResult;
import com.github.aiassistant.entity.model.chat.WebSearchResultVO;
import com.github.aiassistant.service.text.tools.functioncall.UrlReadTools;
import com.github.aiassistant.util.Lists;
import com.github.aiassistant.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

/**
 * 增删改查-联网搜索
 */
public class AiChatWebsearchServiceImpl {
    /**
     * 最大每秒1W的并发量
     */
    private static final int CONCURRENT_QPS = 10000 / 3;
    private static final Logger log = LoggerFactory.getLogger(AiChatWebsearchServiceImpl.class);
    private final LinkedBlockingQueue<AiChatWebsearchRequest> insertRequestQueue = new LinkedBlockingQueue<>(CONCURRENT_QPS);

    // @Resource
    private final AiChatWebsearchMapper aiChatWebsearchMapper;
    // @Resource
    private final AiChatWebsearchResultMapper aiChatWebsearchResultMapper;
    // @Resource
    private final AiChatHistoryMapper aiChatHistoryMapper;
    private int insertPartitionSize = 100;

    public AiChatWebsearchServiceImpl(AiChatWebsearchMapper aiChatWebsearchMapper, AiChatWebsearchResultMapper aiChatWebsearchResultMapper, AiChatHistoryMapper aiChatHistoryMapper) {
        this.aiChatWebsearchMapper = aiChatWebsearchMapper;
        this.aiChatWebsearchResultMapper = aiChatWebsearchResultMapper;
        this.aiChatHistoryMapper = aiChatHistoryMapper;
        // 批量持久化（防止问答过猛）
        InsertBatchThread insertBatchThread = new InsertBatchThread();
        insertBatchThread.setName(getClass().getSimpleName() + "#insertBatch" + insertBatchThread.getId());
        insertBatchThread.start();
    }

    /**
     * 提交聊天记录
     *
     * @param sourceEnum           sourceEnum
     * @param providerName         providerName
     * @param question             question
     * @param resultVO             resultVO
     * @param cost                 cost
     * @param userQueryTraceNumber userQueryTraceNumber
     * @param user                 user
     * @return 提交结果
     */
    public CompletableFuture<AiChatWebsearchRequest> insert(String sourceEnum, String providerName,
                                                            String question, WebSearchResultVO resultVO, long cost,
                                                            String userQueryTraceNumber,
                                                            CompletableFuture<AiChatHistoryServiceImpl.AiChatRequest> user) {
        try {
            // 提交聊天记录
            AiChatWebsearchRequest aiChatRequest = new AiChatWebsearchRequest(resultVO, user);
            List<UrlReadTools.ProxyVO> proxyList = resultVO.getProxyList();
            String proxyString = proxyList != null ? proxyList.stream().map(e -> e == null ? UrlReadTools.NO_PROXY : e.toAddressString()).filter(StringUtils::hasText).collect(Collectors.joining(",")) : "";
            aiChatRequest.setSearchProxy(StringUtils.left(proxyString, 255, true));
            aiChatRequest.setSearchTimeMs(cost);
            aiChatRequest.setUserQueryTraceNumber(userQueryTraceNumber);
            aiChatRequest.setProviderName(providerName);
            aiChatRequest.setQuestion(question);
            aiChatRequest.setSourceEnum(StringUtils.left(sourceEnum, 30, true));
            aiChatRequest.setCreateTime(new Date());
            // 防止问答过猛
            while (!insertRequestQueue.offer(aiChatRequest)) {
                List<AiChatWebsearchRequest> list = new ArrayList<>(insertRequestQueue.size());
                insertRequestQueue.drainTo(list);
                insert(list);
            }
            return aiChatRequest.future;
        } catch (Exception e) {
            CompletableFuture<AiChatWebsearchRequest> future = new CompletableFuture<>();
            future.completeExceptionally(e);
            return future;
        }
    }

    /**
     * 持久化联网结果
     */
    private void insert(List<AiChatWebsearchRequest> list) {
        Collection<AiChatWebsearchRequest> errorList = Collections.newSetFromMap(new IdentityHashMap<>());
        Map<Boolean, List<AiChatWebsearchRequest>> doneListMap = list.stream().collect(Collectors.groupingBy(e -> e.user.isDone()));
        List<AiChatWebsearchRequest> undoneList = doneListMap.get(Boolean.FALSE);
        if (undoneList != null) {
            Map<CompletableFuture<AiChatHistoryServiceImpl.AiChatRequest>, List<AiChatWebsearchRequest>> userGroupMap = undoneList.stream()
                    .collect(Collectors.groupingBy(e -> e.user, IdentityHashMap::new, Collectors.toList()));
            // 相同的future，保持组内一起
            for (Map.Entry<CompletableFuture<AiChatHistoryServiceImpl.AiChatRequest>, List<AiChatWebsearchRequest>> entry : userGroupMap.entrySet()) {
                // 相同的future，保持组内一起
                ArrayList<AiChatWebsearchRequest> values = new ArrayList<>(entry.getValue());
                entry.getKey().whenComplete((unused, throwable) -> {
                    List<AiChatWebsearchRequest> noOffer = null;
                    for (AiChatWebsearchRequest value : values) {
                        if (!insertRequestQueue.offer(value)) {
                            if (noOffer == null) {
                                noOffer = new ArrayList<>(values.size());
                            }
                            noOffer.add(value);
                        }
                    }
                    if (noOffer != null) {
                        insert(noOffer);
                    }
                });
            }
        }

        Collection<AiChatWebsearchRequest> doneList = doneListMap.getOrDefault(Boolean.TRUE, Collections.emptyList());
        for (AiChatWebsearchRequest request : doneList) {
            try {
                AiChatHistoryServiceImpl.AiChatRequest user = request.user.get();
                Integer userChatHistoryId = user.getUserChatHistoryId(aiChatHistoryMapper).get();
                request.setAiChatId(user.getId());
                request.setUserChatHistoryId(userChatHistoryId);

                for (WebSearchResultVO.Row row : request.webSearchResult.getList()) {
                    AiChatWebsearchResult insertResult = new AiChatWebsearchResult();

                    insertResult.setAiChatId(request.getAiChatId());
                    insertResult.setUserChatHistoryId(request.getUserChatHistoryId());
                    insertResult.setPageUrl(StringUtils.left(row.getUrl(), 255, true));
                    insertResult.setPageTitle(StringUtils.left(row.getTitle(), 255, true));
                    insertResult.setPageTime(StringUtils.left(row.getTime(), 50, true));
                    insertResult.setPageSource(StringUtils.left(row.getSource(), 50, true));
                    insertResult.setPageContent(StringUtils.left(row.getContent(), 65000, true));
                    Long urlReadTimeCost = row.getUrlReadTimeCost();
                    insertResult.setUrlReadTimeCost(urlReadTimeCost == null ? 0L : urlReadTimeCost);
                    UrlReadTools.ProxyVO proxyVO = row.getProxy();
                    insertResult.setUrlReadProxy(StringUtils.left(proxyVO != null ? proxyVO.toAddressString() : UrlReadTools.NO_PROXY, 35, true));
                    request.resultList.add(insertResult);
                }
            } catch (Exception e) {
                request.future.completeExceptionally(e);
                errorList.add(request);
            }
        }
        List<AiChatWebsearchRequest> insert = doneList.stream().filter(e -> !errorList.contains(e)).collect(Collectors.toList());
        try {
            Lists.partition(insert, insertPartitionSize).forEach(aiChatWebsearchMapper::insertBatchSomeColumn);

            insert.forEach(e -> e.resultList.forEach(e1 -> e1.setAiChatWebsearchId(e.getId())));
            Collection<AiChatWebsearchResult> websearchResults = insert.stream().map(e -> e.resultList).flatMap(Collection::stream).collect(Collectors.toList());
            Lists.partition(websearchResults, insertPartitionSize).forEach(aiChatWebsearchResultMapper::insertBatchSomeColumn);
            for (AiChatWebsearchRequest request : insert) {
                request.future.complete(request);
            }
        } catch (Exception e) {
            for (AiChatWebsearchRequest request : insert) {
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
     * 联网
     */
    // @Data
    public static class AiChatWebsearchRequest extends AiChatWebsearch {
        private final CompletableFuture<AiChatWebsearchRequest> future = new CompletableFuture<>();
        private final WebSearchResultVO webSearchResult;
        private final CompletableFuture<AiChatHistoryServiceImpl.AiChatRequest> user;
        private final List<AiChatWebsearchResult> resultList = new ArrayList<>();

        public AiChatWebsearchRequest(WebSearchResultVO webSearchResult, CompletableFuture<AiChatHistoryServiceImpl.AiChatRequest> user) {
            this.webSearchResult = webSearchResult;
            this.user = user;
        }
    }

    private class InsertBatchThread extends Thread {

        @Override
        public void run() {
            while (!isInterrupted()) {
                List<AiChatWebsearchRequest> list = new ArrayList<>(Math.max(1, insertRequestQueue.size()));
                if (insertRequestQueue.isEmpty()) {
                    try {
                        list.add(insertRequestQueue.take());
                    } catch (InterruptedException e) {
                        log.info("AiChatWebsearchServiceImpl InterruptedException {}", e.toString(), e);
                        return;
                    }
                }
                insertRequestQueue.drainTo(list);
                try {
                    insert(list);
                    if (log.isDebugEnabled()) {
                        log.debug("AiChatWebsearchServiceImpl insert request queue  {}", list.size());
                    }
                } catch (Exception e) {
                    log.error("AiChatWebsearchServiceImpl insert request queue error {}", e.toString(), e);
                }
            }
        }
    }

}
