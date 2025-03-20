package com.github.aiassistant.dao;

import com.github.aiassistant.entity.AiAssistant;

import java.io.Serializable;
import java.util.List;

public interface AiAssistantMapper {
    AiAssistant selectById(Serializable id);
    List<AiAssistant> selectEnableList(Integer status);

}
