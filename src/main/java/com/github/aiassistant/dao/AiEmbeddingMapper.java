package com.github.aiassistant.dao;

import com.github.aiassistant.entity.AiEmbedding;

import java.util.Collection;
import java.util.List;

public interface AiEmbeddingMapper {
    int insertIgnoreBatchSomeColumn(Collection<? extends AiEmbedding> collection);

    List<AiEmbedding> selectListByMd5(Collection<String> md5,
                                      String modelName,
                                      Integer dimensions);
}
