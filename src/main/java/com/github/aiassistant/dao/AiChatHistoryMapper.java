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
     * 查询问答轮数
     *
     * @param uidType   用户类型
     * @param uid       用户id
     * @param maxRounds 查几轮就够了（减少查询数据量）
     * @return true=满足轮数
     */
    int selectQaRounds(String uidType, Serializable uid, Integer maxRounds);

    List<String> selectAgainTraceNumberList(List<String> userQueryTraceNumberList);

    List<AiChatHistory> selectRootAgainTraceNumberList(List<String> userQueryTraceNumberList);

    String selectLastUserAgainTraceNumber(Integer chatId);

}
