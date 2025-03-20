package com.github.aiassistant.dao;

import com.github.aiassistant.entity.AiJsonschema;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

public interface AiAssistantJsonschemaMapper {
    List<AiJsonschema> selectBatchIds(Collection<? extends Serializable> idList);

}
