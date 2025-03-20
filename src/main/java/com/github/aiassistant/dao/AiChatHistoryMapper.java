package com.github.aiassistant.dao;

import com.github.aiassistant.entity.AiChatHistory;
import com.github.aiassistant.entity.model.chat.AiChatHistoryResp;

import java.io.Serializable;
import java.util.Collection;
import java.util.Date;
import java.util.List;

public interface AiChatHistoryMapper {
    List<AiChatHistory> selectBatchIds(Collection<? extends Serializable> idList);

    int insertBatchSomeColumn(Collection<? extends AiChatHistory> collection);

    Integer selectLastUserChatHistoryId(Integer chatId, String againUserQueryTraceNumber);

    int updateUserChatHistoryIdByIds(List<Integer> idList, Integer userChatHistoryId);

    int updateDeleteByUserQueryTraceNumber(Date deleteTime,
                                           List<String> againUserQueryTraceNumberList);

    List<AiChatHistoryResp> selectListByChatId(Integer aiChatId);

    int sumTodayCharLength(String uidType, Serializable uid);

    List<String> selectAgainTraceNumberList(List<String> userQueryTraceNumberList);

    List<AiChatHistory> selectRootAgainTraceNumberList(List<String> userQueryTraceNumberList);

    String selectLastUserAgainTraceNumber(Integer chatId);

}
