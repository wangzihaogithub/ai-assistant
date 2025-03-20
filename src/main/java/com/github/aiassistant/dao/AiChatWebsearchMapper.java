package com.github.aiassistant.dao;

import com.github.aiassistant.entity.AiChatWebsearch;
import com.github.aiassistant.entity.model.chat.AiChatWebsearchResp;

import java.util.Collection;
import java.util.List;

public interface AiChatWebsearchMapper {
    int insertBatchSomeColumn(Collection<? extends AiChatWebsearch> collection);

    List<AiChatWebsearchResp> selectListByChatId(Integer chatId);
}
