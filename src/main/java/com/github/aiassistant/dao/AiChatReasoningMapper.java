package com.github.aiassistant.dao;

import com.github.aiassistant.entity.AiChatReasoning;

import java.util.Collection;

public interface AiChatReasoningMapper {
    int insertBatchSomeColumn(Collection<? extends AiChatReasoning> collection);

}
