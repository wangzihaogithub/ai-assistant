package com.github.aiassistant.service.text;

import com.github.aiassistant.dao.AiAssistantMapper;
import com.github.aiassistant.entity.AiAssistant;
import com.github.aiassistant.enums.AiAssistantStatusEnum;
import com.github.aiassistant.util.StringUtils;

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
     *
     * @param statusEnum 状态
     * @return 智能体
     */
    public List<AiAssistant> selectList(AiAssistantStatusEnum statusEnum) {
        return aiAssistantMapper.selectEnableList(statusEnum == null ? null : statusEnum.getCode());
    }

    /**
     * 查询智能体
     *
     * @param id id
     * @return 智能体
     */
    public AiAssistant selectById(String id) {
        if (StringUtils.hasText(id)) {
            return aiAssistantMapper.selectById(id);
        } else {
            return null;
        }
    }
}
