package com.github.aiassistant.dao;

import com.github.aiassistant.entity.AiChat;
import com.github.aiassistant.entity.model.chat.AiChatListResp;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

public interface AiChatMapper {
    AiChat selectById(Serializable id);

    int insert(AiChat row);

    int updateNameById(Integer id, String name, Date updateTime);

    int updateDeleteTimeById(Integer id, Date deleteTime);


    /**
     * 更新最近聊天时间
     */
    int updateLastChatTime(List<Integer> idList,
                           Date lastChatTime);

    /**
     * 更新最近联网状态
     */
    int updateLastWebsearchFlag(List<Integer> idList,
                                Boolean lastWebsearch);

    List<AiChatListResp> selectListByUid(String keyword,
                                         Integer offset, Integer pageSize,
                                         String uidType, Serializable createUid);
}
