package com.github.aiassistant.dao;

import com.github.aiassistant.entity.AiToolParameter;

import java.util.Collection;
import java.util.List;

public interface AiToolParameterMapper {
    List<AiToolParameter> selectListByToolId(Collection<Integer> toolId);
}
