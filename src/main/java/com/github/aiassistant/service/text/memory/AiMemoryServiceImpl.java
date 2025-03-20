package com.github.aiassistant.service.text.memory;

import com.github.aiassistant.dao.AiMemoryMapper;
import com.github.aiassistant.entity.AiMemory;

import java.util.Date;

/**
 * 增删改查-AI记忆
 */
public class AiMemoryServiceImpl {
    // @Resource
    private final AiMemoryMapper aiMemoryMapper;

    public AiMemoryServiceImpl(AiMemoryMapper aiMemoryMapper) {
        this.aiMemoryMapper = aiMemoryMapper;
    }

    /**
     * 新增记忆
     * @param now now
     * @return 记忆
     */
    public AiMemory insert(Date now) {
        AiMemory aiMemory = new AiMemory();
        aiMemory.setCreateTime(now);
        aiMemory.setUpdateTime(now);
        aiMemory.setUserTokenCount(0);
        aiMemory.setAiTokenCount(0);
        aiMemory.setKnowledgeTokenCount(0);
        aiMemory.setUserCharLength(0);
        aiMemory.setKnowledgeCharLength(0);
        aiMemory.setAiCharLength(0);
        aiMemoryMapper.insert(aiMemory);
        return aiMemory;
    }
}
