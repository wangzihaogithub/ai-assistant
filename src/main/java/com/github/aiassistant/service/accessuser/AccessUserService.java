package com.github.aiassistant.service.accessuser;

import com.github.aiassistant.dao.*;
import com.github.aiassistant.entity.AiAssistant;
import com.github.aiassistant.entity.AiAssistantKn;
import com.github.aiassistant.entity.AiChat;
import com.github.aiassistant.entity.model.chat.MemoryIdVO;
import com.github.aiassistant.entity.model.user.AiAccessUserVO;
import com.github.aiassistant.enums.AiChatUidTypeEnum;
import com.github.aiassistant.service.text.tools.AiToolServiceImpl;
import com.github.aiassistant.serviceintercept.AccessUserServiceIntercept;
import com.github.aiassistant.util.AiUtil;
import com.github.aiassistant.util.StringUtils;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * AI的用户服务
 */
// @Component
public class AccessUserService {

    // @Resource
    private final AiChatMapper aiChatMapper;
    // @Resource
    private final AiAssistantJsonschemaMapper aiAssistantJsonschemaMapper;
    // @Resource
    private final AiAssistantMstateMapper aiAssistantMstateMapper;
    // @Resource
    private final AiAssistantMapper aiAssistantMapper;
    // @Resource
    private final AiAssistantKnMapper aiAssistantKnMapper;
    // @Resource
    private final AiAssistantFewshotMapper aiAssistantFewshotMapper;
    // @Autowired
    private final AiToolServiceImpl aiToolService;

    private final Supplier<Collection<AccessUserServiceIntercept>> interceptList;

    public AccessUserService(AiChatMapper aiChatMapper,
                             AiAssistantJsonschemaMapper aiAssistantJsonschemaMapper,
                             AiAssistantMstateMapper aiAssistantMstateMapper,
                             AiAssistantMapper aiAssistantMapper,
                             AiAssistantKnMapper aiAssistantKnMapper,
                             AiAssistantFewshotMapper aiAssistantFewshotMapper,
                             AiToolServiceImpl aiToolService,
                             Supplier<Collection<AccessUserServiceIntercept>> interceptList) {
        this.aiChatMapper = aiChatMapper;
        this.aiAssistantJsonschemaMapper = aiAssistantJsonschemaMapper;
        this.aiAssistantMstateMapper = aiAssistantMstateMapper;
        this.aiAssistantMapper = aiAssistantMapper;
        this.aiAssistantKnMapper = aiAssistantKnMapper;
        this.aiAssistantFewshotMapper = aiAssistantFewshotMapper;
        this.aiToolService = aiToolService;
        this.interceptList = interceptList;
    }

    /**
     * 是否有权限
     * @param chatId chatId
     * @param createUid createUid
     * @param uidTypeEnum uidTypeEnum
     * @return 有权限
     */
    public boolean hasPermission(Serializable chatId, Serializable createUid, AiChatUidTypeEnum uidTypeEnum) {
        if (chatId == null) {
            return false;
        }
        AiChat aiChat = aiChatMapper.selectById(chatId);
        boolean b = uidTypeEnum.hasPermission(aiChat, createUid);
        for (AccessUserServiceIntercept intercept : interceptList.get()) {
            b = intercept.hasPermission(chatId, createUid, uidTypeEnum);
            if (!b) {
                return false;
            }
        }
        return b;
    }

    /**
     * 获取记忆ID
     * @param chatId chatId
     * @param createUid createUid
     * @param uidTypeEnum uidTypeEnum
     * @return 记忆ID
     */
    public MemoryIdVO getMemoryId(Serializable chatId, Serializable createUid, AiChatUidTypeEnum uidTypeEnum) {
        if (chatId == null) {
            return null;
        }
        AiChat aiChat = aiChatMapper.selectById(chatId);
        if (uidTypeEnum.hasPermission(aiChat, createUid)) {
            AiAssistant assistant = aiAssistantMapper.selectById(aiChat.getAssistantId());
            String aiJsonschemaIds = assistant.getAiJsonschemaIds();
            MemoryIdVO vo = new MemoryIdVO();
            vo.setAiChat(aiChat);
            vo.setAiAssistant(assistant);
            if (StringUtils.hasText(aiJsonschemaIds)) {
                vo.setJsonschemaList(aiAssistantJsonschemaMapper.selectBatchIds(Arrays.asList(aiJsonschemaIds.split(","))));
            }
            vo.setMstateList(aiAssistantMstateMapper.selectListByAssistantId(assistant.getId()));
            vo.setAssistantKnMap(aiAssistantKnMapper.selectListByAssistantId(assistant.getId()).stream()
                    .collect(Collectors.groupingBy(AiAssistantKn::getKnTypeEnum)));
            vo.setFewshotList(aiAssistantFewshotMapper.selectListByAssistantId(assistant.getId()));
            vo.setToolMethodList(aiToolService.selectToolMethodList(AiUtil.splitString(assistant.getAiToolIds())));
            for (AccessUserServiceIntercept intercept : interceptList.get()) {
                vo = intercept.afterMemoryId(vo, chatId, createUid, uidTypeEnum);
            }
            return vo;
        } else {
            return null;
        }
    }

    /**
     * 获取用户ID
     * @return 用户ID
     */
    public Serializable getCurrentUserId() {
        for (AccessUserServiceIntercept intercept : interceptList.get()) {
            Serializable currentUserId = intercept.getCurrentUserId();
            if (currentUserId != null) {
                return currentUserId;
            }
        }
        return null;
    }

    /**
     * 获取用户
     * @return 用户
     */
    public AiAccessUserVO getCurrentUser() {
        for (AccessUserServiceIntercept intercept : interceptList.get()) {
            AiAccessUserVO currentUser = intercept.getCurrentUser();
            if (currentUser != null) {
                return currentUser;
            }
        }
        return null;
    }

}
