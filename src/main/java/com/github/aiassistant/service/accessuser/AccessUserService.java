package com.github.aiassistant.service.accessuser;

import com.github.aiassistant.dao.AiAssistantKnMapper;
import com.github.aiassistant.dao.AiAssistantMapper;
import com.github.aiassistant.dao.AiChatMapper;
import com.github.aiassistant.entity.AiAssistantKn;
import com.github.aiassistant.entity.AiChat;
import com.github.aiassistant.entity.model.chat.AiChatQuestionPermissionResp;
import com.github.aiassistant.entity.model.chat.AiChatTokenVO;
import com.github.aiassistant.entity.model.chat.ChatQueryReq;
import com.github.aiassistant.entity.model.chat.MemoryIdVO;
import com.github.aiassistant.entity.model.user.AiAccessUserVO;
import com.github.aiassistant.enums.AiChatUidTypeEnum;
import com.github.aiassistant.service.text.chat.AiChatHistoryServiceImpl;
import com.github.aiassistant.serviceintercept.AccessUserServiceIntercept;

import java.io.Serializable;
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
    private final AiAssistantMapper aiAssistantMapper;
    // @Resource
    private final AiAssistantKnMapper aiAssistantKnMapper;

    // @Autowired
    private final AiChatHistoryServiceImpl aiChatHistoryService;
    private final Supplier<Collection<AccessUserServiceIntercept>> interceptList;

    public AccessUserService(AiChatMapper aiChatMapper,
                             AiAssistantMapper aiAssistantMapper,
                             AiAssistantKnMapper aiAssistantKnMapper,
                             AiChatHistoryServiceImpl aiChatHistoryService,
                             Supplier<Collection<AccessUserServiceIntercept>> interceptList) {
        this.aiChatMapper = aiChatMapper;
        this.aiAssistantMapper = aiAssistantMapper;
        this.aiAssistantKnMapper = aiAssistantKnMapper;
        this.aiChatHistoryService = aiChatHistoryService;
        this.interceptList = interceptList;
    }

    /**
     * 是否有权限
     *
     * @param chatId      chatId
     * @param createUid   createUid
     * @param uidTypeEnum uidTypeEnum
     * @return 有权限
     */
    public boolean hasPermission(Integer chatId, Serializable createUid, AiChatUidTypeEnum uidTypeEnum) {
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
     * 获取下次提问权限
     *
     * @param chatId        chatId
     * @param createUid     createUid
     * @param uidTypeEnum   uidTypeEnum
     * @param maxTokenCount maxTokenCount
     * @return 提问权限
     */
    public AiChatQuestionPermissionResp getQuestionPermission(Integer chatId, Serializable createUid, AiChatUidTypeEnum uidTypeEnum, int maxTokenCount) {
        AiChatTokenVO token = aiChatHistoryService.sumTodayCharLength(createUid, uidTypeEnum);

        AiChatQuestionPermissionResp resp = new AiChatQuestionPermissionResp();
        resp.setTokenCount(token.getTokenCount());
        resp.setHasTokens(token.isHasTokens(maxTokenCount));
        resp.setHasPermission(chatId == null || hasPermission(chatId, createUid, uidTypeEnum));
        resp.setUserQueryTraceNumber(ChatQueryReq.newUserQueryTraceNumber());
        resp.setTimestamp(System.currentTimeMillis());
        return resp;
    }

    /**
     * 获取记忆ID
     *
     * @param chatId      chatId
     * @param createUid   createUid
     * @param uidTypeEnum uidTypeEnum
     * @return 记忆ID
     */
    public MemoryIdVO getMemoryId(Serializable chatId, Serializable createUid, AiChatUidTypeEnum uidTypeEnum) {
        if (chatId == null) {
            return null;
        }
        AiChat aiChat = aiChatMapper.selectById(chatId);
        if (uidTypeEnum.hasPermission(aiChat, createUid)) {
            MemoryIdVO vo = new MemoryIdVO();
            vo.setAiChat(aiChat);
            vo.setAiAssistant(aiAssistantMapper.selectById(aiChat.getAssistantId()));
            vo.setAssistantKnMap(aiAssistantKnMapper.selectListByAssistantId(aiChat.getAssistantId()).stream()
                    .collect(Collectors.groupingBy(AiAssistantKn::getKnTypeEnum)));
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
     *
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
     *
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
