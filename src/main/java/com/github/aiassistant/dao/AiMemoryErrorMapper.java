package com.github.aiassistant.dao;

import com.github.aiassistant.entity.AiMemoryError;

import java.util.List;

public interface AiMemoryErrorMapper {
    int insert(AiMemoryError row);

    List<AiMemoryError> selectListByChatId(Integer chatId);

}
