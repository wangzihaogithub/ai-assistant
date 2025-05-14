package com.github.aiassistant.dao;

import com.github.aiassistant.entity.AiMemoryMessageMetadata;

import java.util.Collection;
import java.util.List;

public interface AiMemoryMessageMetadataMapper {

    int insertBatchSomeColumn(Collection<? extends AiMemoryMessageMetadata> collection);

    List<AiMemoryMessageMetadata> selectListByChatId(Integer aiChatId);
}
