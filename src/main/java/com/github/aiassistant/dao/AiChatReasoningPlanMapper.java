package com.github.aiassistant.dao;

import com.github.aiassistant.entity.AiChatReasoningPlan;

import java.util.Collection;

public interface AiChatReasoningPlanMapper {
    int insertBatchSomeColumn(Collection<? extends AiChatReasoningPlan> collection);

}
