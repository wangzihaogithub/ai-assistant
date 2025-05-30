package com.github.aiassistant.dao;

import com.github.aiassistant.entity.AiMemorySearch;

import java.util.Collection;

public interface AiMemorySearchMapper {
    int insertBatchSomeColumn(Collection<? extends AiMemorySearch> collection);

}
