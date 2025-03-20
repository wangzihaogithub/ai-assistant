package com.github.aiassistant.dao;

import com.github.aiassistant.entity.AiChatAbort;

import java.util.List;

public interface AiChatAbortMapper {
    int insert(AiChatAbort row);

    List<AiChatAbort> selectListByChatId(Integer aiChatId);

    int updateRootAgainUserQueryTraceNumber(String userQueryTraceNumber, String rootAgainUserQueryTraceNumber);
}
