package com.github.aiassistant.dao;

import com.github.aiassistant.entity.AiAssistantFewshot;

import java.util.List;

public interface AiAssistantFewshotMapper {
    List<AiAssistantFewshot> selectListByAssistantId(String aiAssistantId);
}
