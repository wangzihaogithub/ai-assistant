package com.github.aiassistant.dao;

import com.github.aiassistant.entity.AiMemoryMessageKn;

import java.util.Collection;

public interface AiMemoryMessageKnMapper {
    int insertBatchSomeColumn(Collection<? extends AiMemoryMessageKn> collection);
}
