package com.github.aiassistant.dao;

import com.github.aiassistant.entity.AiMemoryRagDoc;

import java.util.Collection;

public interface AiMemoryRagDocMapper {
    int insertBatchSomeColumn(Collection<? extends AiMemoryRagDoc> collection);

}
