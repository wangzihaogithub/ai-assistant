package com.github.aiassistant.dao;

import com.github.aiassistant.entity.AiMemoryRag;

import java.util.Collection;

public interface AiMemoryRagMapper {
    int insertBatchSomeColumn(Collection<? extends AiMemoryRag> collection);

}
