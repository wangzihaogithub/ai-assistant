package com.github.aiassistant.dao;

import com.github.aiassistant.entity.AiChatWebsearchResult;

import java.util.Collection;

public interface AiChatWebsearchResultMapper {
    int insertBatchSomeColumn(Collection<? extends AiChatWebsearchResult> collection);

}
