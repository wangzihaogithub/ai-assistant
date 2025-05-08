package com.github.aiassistant.serviceintercept;

import com.github.aiassistant.entity.AiChatAbort;
import com.github.aiassistant.entity.AiMemoryError;
import com.github.aiassistant.entity.model.chat.*;
import com.github.aiassistant.entity.model.user.AiAccessUserVO;
import com.github.aiassistant.service.text.chat.AiChatHistoryServiceImpl;

import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface AiChatHistoryServiceIntercept extends ServiceIntercept {
    default List<AiChatHistoryResp> beforeSelectListByChatId(Integer aiChatId,
                                                             List<AiChatHistoryResp> list,
                                                             List<AiChatAbort> abortList,
                                                             List<AiMemoryError> memoryErrorList,
                                                             List<AiChatWebsearchResp> websearchList) {
        return list;
    }

    default List<AiUserChatHistoryResp> afterSelectListByChatId(Integer aiChatId,
                                                                List<AiUserChatHistoryResp> result,
                                                                List<AiChatHistoryResp> list,
                                                                List<AiChatAbort> abortList,
                                                                List<AiMemoryError> memoryErrorList,
                                                                List<AiChatWebsearchResp> websearchList) {
        return result;
    }

    default AiChatHistoryServiceImpl.AiChatHistoryVO afterMessage(AiChatHistoryServiceImpl.AiChatHistoryVO source, Date now, RequestTraceVO<MemoryIdVO, AiAccessUserVO> requestTrace,
                                                                  String againUserQueryTraceNumber, Boolean websearch,
                                                                  CompletableFuture<AiChatHistoryServiceImpl.AiChatRequest> user,
                                                                  MessageVO<AiAccessUserVO> message) {
        return source;
    }


    default AiChatHistoryServiceImpl.AiChatRequest afterChat(AiChatHistoryServiceImpl.AiChatRequest source, Date now, RequestTraceVO<MemoryIdVO, AiAccessUserVO> requestTrace,
                                                             String againUserQueryTraceNumber, Boolean websearch,
                                                             CompletableFuture<AiChatHistoryServiceImpl.AiChatRequest> user) {
        return source;
    }

    default void afterInsert(List<AiChatHistoryServiceImpl.AiChatRequest> list) {
        // 插入推荐岗位
//        for (AiChatHistoryVO historyVO : historyList) {
//            // 回填历史ID
//            historyVO.getJobList().forEach(e -> e.setAiChatHistoryId(historyVO.getId()));
//        }
//        aiChatHistoryJobMapper.insertBatchSomeColumnDefault(historyList.stream().flatMap(e -> e.getJobList().stream()).collect(Collectors.toList()));

    }

}
