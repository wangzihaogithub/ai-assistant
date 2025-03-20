package com.github.aiassistant.service.text;

// import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
// import com.baomidou.mybatisplus.core.toolkit.Wrappers;

import com.github.aiassistant.dao.AiAssistantMapper;
import com.github.aiassistant.entity.AiAssistant;
import com.github.aiassistant.enums.AiAssistantStatusEnum;

import java.util.List;

/**
 * 增删改查-智能体
 */
public class AiAssistantServiceImpl {
    // @Resource
    private final AiAssistantMapper aiAssistantMapper;

    public AiAssistantServiceImpl(AiAssistantMapper aiAssistantMapper) {
        this.aiAssistantMapper = aiAssistantMapper;
    }

    /**
     * 查询智能体
     */
    public List<AiAssistant> selectList(AiAssistantStatusEnum statusEnum) {
        return aiAssistantMapper.selectEnableList(statusEnum == null ? null : statusEnum.getCode());
    }

}
