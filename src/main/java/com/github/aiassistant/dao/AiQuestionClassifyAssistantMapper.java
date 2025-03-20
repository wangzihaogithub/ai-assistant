package com.github.aiassistant.dao;

import com.github.aiassistant.entity.AiQuestionClassifyAssistant;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

public interface AiQuestionClassifyAssistantMapper {
    List<AiQuestionClassifyAssistant> selectBatchIds(Collection<? extends Serializable> idList);

}
