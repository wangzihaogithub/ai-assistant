package com.github.aiassistant.dao;

import com.github.aiassistant.entity.AiAssistantMstate;

import java.util.List;

public interface AiAssistantMstateMapper {
    List<AiAssistantMstate> selectListByAssistantId(String assistantId);
}
