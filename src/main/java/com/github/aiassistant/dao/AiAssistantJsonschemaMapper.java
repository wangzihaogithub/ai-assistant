package com.github.aiassistant.dao;

import com.github.aiassistant.entity.AiJsonschema;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public interface AiAssistantJsonschemaMapper {
    List<AiJsonschema> selectBatchIds(Collection<? extends Serializable> idList);

    default AiJsonschema selectById(Serializable id) {
        if (id != null) {
            List<AiJsonschema> list = selectBatchIds(Collections.singletonList(id));
            return list == null || list.isEmpty() ? null : list.get(0);
        } else {
            return null;
        }
    }

}
