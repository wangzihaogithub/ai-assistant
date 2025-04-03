package com.github.aiassistant.service.text.memory;

import com.github.aiassistant.dao.AiMemoryMstateMapper;
import com.github.aiassistant.entity.AiMemoryMstate;
import com.github.aiassistant.entity.model.chat.MStateAiParseVO;
import com.github.aiassistant.entity.model.chat.MStateVO;
import com.github.aiassistant.util.Lists;
import com.github.aiassistant.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;

public class AiMemoryMstateServiceImpl {
    /**
     * 最大每秒1W的并发量
     */
    private static final int CONCURRENT_QPS = 10000 / 3;
    private static final Logger log = LoggerFactory.getLogger(AiMemoryMstateServiceImpl.class);
    private final LinkedBlockingQueue<MstateVO> insertRequestQueue = new LinkedBlockingQueue<>(CONCURRENT_QPS);
    private final AiMemoryMstateMapper aiMemoryMstateMapper;
    private int insertPartitionSize = 100;

    public AiMemoryMstateServiceImpl(AiMemoryMstateMapper aiMemoryMstateMapper) {
        this.aiMemoryMstateMapper = aiMemoryMstateMapper;
        // 批量持久化（防止问答过猛）
        InsertBatchThread insertBatchThread = new InsertBatchThread();
        insertBatchThread.setName(getClass().getSimpleName() + "#insertBatch" + insertBatchThread.getId());
        insertBatchThread.start();
    }

    private static List<AiMemoryMstate> buildInsertList(Map<String, Object> stateMap, Integer aiMemoryId,
                                                        AiMemoryMessageServiceImpl.AiMemoryMessageVO userMsg, Date now,
                                                        boolean known) {
        if (stateMap == null || stateMap.isEmpty()) {
            return new ArrayList<>();
        }
        List<AiMemoryMstate> insertList = new ArrayList<>();
        for (Map.Entry<String, Object> entry : stateMap.entrySet()) {
            Object value = entry.getValue();
            if (value == null || "".equals(value)) {
                continue;
            }
            AiMemoryMstate mstate = new AiMemoryMstate();
            mstate.setAiMemoryId(aiMemoryId);
            mstate.setStateKey(StringUtils.left(entry.getKey(), 50, true));
            mstate.setStateValue(StringUtils.left(Objects.toString(value), 65000, true));
            mstate.setUserAiMemoryMessageId(userMsg.getId());
            mstate.setUserMessageIndex(userMsg.getMessageIndex());
            mstate.setCreateTime(now);
            mstate.setKnownFlag(known);
            mstate.setUserQueryTraceNumber(userMsg.getUserQueryTraceNumber());
            insertList.add(mstate);
        }
        return insertList;
    }

    public MStateVO selectMstate(Integer memoryId) {
        List<AiMemoryMstate> mstateList = aiMemoryMstateMapper.selectLastByAiMemoryId(memoryId);
        if (mstateList.isEmpty()) {
            return null;
        }
        Map<String, String> knownState = new LinkedHashMap<>();
        Map<String, String> unknownState = new LinkedHashMap<>();
        for (AiMemoryMstate mstate : mstateList) {
            String stateKey = mstate.getStateKey();
            String stateValue = mstate.getStateValue();
            if (Boolean.TRUE.equals(mstate.getKnownFlag())) {
                knownState.put(stateKey, stateValue);
            } else {
                unknownState.put(stateKey, stateValue);
            }
        }
        return new MStateVO(knownState, unknownState);
    }

    public CompletableFuture<MstateVO> insert(AiMemoryMessageServiceImpl.AiMemoryVO memoryVO,
                                              MStateAiParseVO parseVO) {
        CompletableFuture<MstateVO> future = new CompletableFuture<>();
        MstateVO vo = new MstateVO(future, parseVO, memoryVO);
        if (!insertRequestQueue.offer(vo)) {
            log.warn("AiMemoryMstateServiceImpl insertRequestQueue offer fail! memoryId = {}", memoryVO.getId());
            future.complete(vo);
        }
        return future;
    }

    private void insert(List<MstateVO> list) {
        List<AiMemoryMstate> insertList = new ArrayList<>();
        for (MstateVO vo : list) {
            AiMemoryMessageServiceImpl.AiMemoryMessageVO userMsg = vo.memoryVO.getMessageList().stream().filter(e -> Boolean.TRUE.equals(e.getUserQueryFlag())).findFirst().orElse(null);
            if (userMsg == null) {
                // 重新回答没有用户消息
                vo.future.complete(vo);
            } else {
                Date now = new Date();
                try {
                    Map<String, Object> knownStateToMap = vo.state.getKnownState().get();
                    insertList.addAll(buildInsertList(knownStateToMap, vo.memoryVO.getId(), userMsg, now, true));

                    Map<String, Object> unknownStateToMap = vo.state.getUnknownState().get();
                    insertList.addAll(buildInsertList(unknownStateToMap, vo.memoryVO.getId(), userMsg, now, false));
                } catch (Exception e) {
                    vo.future.completeExceptionally(e);
                }
            }
        }
        Lists.partition(insertList, insertPartitionSize).forEach(aiMemoryMstateMapper::insertIgnoreBatchSomeColumn);
        for (MstateVO vo : list) {
            if (!vo.future.isDone()) {
                vo.future.complete(vo);
            }
        }
    }

    public int getInsertPartitionSize() {
        return insertPartitionSize;
    }

    public void setInsertPartitionSize(int insertPartitionSize) {
        this.insertPartitionSize = insertPartitionSize;
    }

    public static class MstateVO {
        final CompletableFuture<MstateVO> future;
        final MStateAiParseVO state;
        final AiMemoryMessageServiceImpl.AiMemoryVO memoryVO;

        public MstateVO(CompletableFuture<MstateVO> future, MStateAiParseVO state, AiMemoryMessageServiceImpl.AiMemoryVO memoryVO) {
            this.future = future;
            this.state = state;
            this.memoryVO = memoryVO;
        }
    }

    private class InsertBatchThread extends Thread {

        @Override
        public void run() {
            while (!isInterrupted()) {
                List<MstateVO> list = new ArrayList<>(Math.max(1, insertRequestQueue.size()));
                if (insertRequestQueue.isEmpty()) {
                    try {
                        list.add(insertRequestQueue.take());
                    } catch (InterruptedException e) {
                        log.info("AiMemoryMstateServiceImpl InterruptedException {}", e.toString(), e);
                        return;
                    }
                } else {
                    insertRequestQueue.drainTo(list);
                }
                try {
                    insert(list);
                } catch (Exception e) {
                    log.error("AiMemoryMstateServiceImpl insert request queue error {}", e.toString(), e);
                }
            }
        }
    }
}
