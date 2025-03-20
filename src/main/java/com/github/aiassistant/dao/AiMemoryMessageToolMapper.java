package com.github.aiassistant.dao;

import com.github.aiassistant.entity.AiMemoryMessageTool;

import java.util.Collection;

public interface AiMemoryMessageToolMapper {

    int insertBatchSomeColumn(Collection<? extends AiMemoryMessageTool> collection);

}
