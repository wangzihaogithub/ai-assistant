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

    /**
     * 是否满足轮数
     *
     * @param uidType 用户类型
     * @param uid     用户id
     * @param rounds  轮数
     * @return true=满足轮数
     */
    boolean selectEnoughRoundsFlag(String uidType, Serializable uid, Integer rounds);

    List<String> selectAgainTraceNumberList(List<String> userQueryTraceNumberList);

    List<AiChatHistory> selectRootAgainTraceNumberList(List<String> userQueryTraceNumberList);

    String selectLastUserAgainTraceNumber(Integer chatId);

}
