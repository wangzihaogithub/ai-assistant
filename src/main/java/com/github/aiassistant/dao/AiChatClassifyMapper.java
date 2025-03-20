package com.github.aiassistant.dao;

import com.github.aiassistant.entity.AiChatClassify;

import java.util.Collection;

public interface AiChatClassifyMapper {
    int insertBatchSomeColumn(Collection<? extends AiChatClassify> collection);

}
