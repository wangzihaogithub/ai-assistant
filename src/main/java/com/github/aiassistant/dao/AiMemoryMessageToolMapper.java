package com.github.aiassistant.dao;

import com.github.aiassistant.entity.AiMemoryMessageTool;

import java.util.Collection;
import java.util.List;

public interface AiMemoryMessageToolMapper {

    int insertBatchSomeColumn(Collection<? extends AiMemoryMessageTool> collection);

    List<AiMemoryMessageTool> selectListByMessageIds(List<Integer> messageIds);
}
