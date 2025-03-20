package com.github.aiassistant.dao;

import com.github.aiassistant.entity.AiQuestionClassify;

import java.util.List;

public interface AiQuestionClassifyMapper {
    List<AiQuestionClassify> selectEnableList(String aiAssistantId);
}
