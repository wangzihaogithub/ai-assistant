package com.github.aiassistant.dao;

import com.github.aiassistant.entity.AiMemorySearchDoc;

import java.util.Collection;

public interface AiMemorySearchDocMapper {
    int insertBatchSomeColumn(Collection<? extends AiMemorySearchDoc> collection);

}
