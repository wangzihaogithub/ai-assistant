package com.github.aiassistant.dao;

import com.github.aiassistant.entity.AiAssistantKn;

import java.util.List;

public interface AiAssistantKnMapper {
    List<AiAssistantKn> selectListByAssistantId(String assistantId);
}
