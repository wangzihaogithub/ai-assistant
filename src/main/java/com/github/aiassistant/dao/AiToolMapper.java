package com.github.aiassistant.dao;

import com.github.aiassistant.entity.AiTool;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

public interface AiToolMapper {

    List<AiTool> selectBatchIds(Collection<? extends Serializable> idList);

}
