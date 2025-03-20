package com.github.aiassistant.dao;

import com.github.aiassistant.entity.AiMemoryMstate;

import java.util.Collection;
import java.util.List;

public interface AiMemoryMstateMapper {
    int insertIgnoreBatchSomeColumn(Collection<? extends AiMemoryMstate> collection);

    List<AiMemoryMstate> selectLastByAiMemoryId(Integer aiMemoryId);
}
