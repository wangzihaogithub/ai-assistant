package com.github.aiassistant.dao;

import com.github.aiassistant.entity.AiMemory;

import java.util.Date;

public interface AiMemoryMapper {
    int insert(AiMemory row);

    int updateTokens(Integer id,
                     Integer userTokenCount,
                     Integer aiTokenCount,
                     Integer knowledgeTokenCount,

                     Integer userCharLength,
                     Integer knowledgeCharLength,
                     Integer aiCharLength,
                     Date updateTime);
}
