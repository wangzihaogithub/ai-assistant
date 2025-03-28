package com.github.aiassistant.service.text.chat;

import com.github.aiassistant.dao.AiChatHistoryMapper;
import com.github.aiassistant.dao.AiChatMapper;
import com.github.aiassistant.entity.AiChat;
import com.github.aiassistant.entity.AiChatHistory;
import com.github.aiassistant.entity.AiMemory;
import com.github.aiassistant.entity.model.chat.AiChatListResp;
import com.github.aiassistant.entity.model.chat.AiChatResp;
import com.github.aiassistant.enums.AiChatTimeEnum;
import com.github.aiassistant.enums.AiChatUidTypeEnum;
import com.github.aiassistant.service.text.memory.AiMemoryServiceImpl;
import com.github.aiassistant.util.StringUtils;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 增删改查-用户和AI聊天的会话
 */
//@Service
public class AiChatServiceImpl {
    // @Resource
    private final AiChatMapper aiChatMapper;
    // @Resource
    private final AiChatHistoryMapper aiChatHistoryMapper;
    // @Resource
    private final AiMemoryServiceImpl aiMemoryService;
    private int nameLimit = 100;

    public AiChatServiceImpl(AiChatMapper aiChatMapper, AiChatHistoryMapper aiChatHistoryMapper,
                             AiMemoryServiceImpl aiMemoryService) {
        this.aiChatMapper = aiChatMapper;
        this.aiChatHistoryMapper = aiChatHistoryMapper;
        this.aiMemoryService = aiMemoryService;
    }

    public int getNameLimit() {
        return nameLimit;
    }

    public void setNameLimit(int nameLimit) {
        this.nameLimit = nameLimit;
    }

    private String nameLimit(String name) {
        return StringUtils.substring(name, nameLimit, true);
    }

    public AiChatResp insert(String assistantId, String name, Serializable createUid, AiChatUidTypeEnum uidTypeEnum) {
        Date now = new Date();

        Integer createUidInt = createUid == null || "".equals(createUid) ? null : Integer.valueOf(createUid.toString());

        AiMemory aiMemory = aiMemoryService.insert(now);

        AiChat aiChat = new AiChat();
        if (StringUtils.hasText(name)) {
            aiChat.setName(nameLimit(name));
        } else {
            aiChat.setName(new SimpleDateFormat("MM-dd HH:mm:ss").format(now) + "的聊天");
        }
        aiChat.setCreateTime(now);
        aiChat.setUpdateTime(now);
        aiChat.setCreateUid(createUidInt);
        aiChat.setAiMemoryId(aiMemory.getId());
        aiChat.setDeleteTime(null);
        aiChat.setAssistantId(assistantId);
        aiChat.setLastChatTime(now);
        aiChat.setUidType(uidTypeEnum.getCode());
        aiChat.setLastWebsearchFlag(false);
        aiChatMapper.insert(aiChat);
        return AiChatResp.convert(aiChat);
    }

    public boolean delete(Integer chatId) {
        return aiChatMapper.updateDeleteTimeById(chatId, new Date()) > 0;
    }

    public List<AiChatListResp> selectList(String keyword,
                                           Integer pageNum,
                                           Integer pageSize,
                                           String startTime,
                                           String endTime,
                                           AiChatTimeEnum chatTimeEnum,
                                           Serializable createUid,
                                           AiChatUidTypeEnum uidTypeEnum) {
        if (chatTimeEnum == null) {
            chatTimeEnum = AiChatTimeEnum.lastChatTime;
        }
        int offset = Math.max(0, (pageNum - 1) * pageSize);
        List<AiChatListResp> list = aiChatMapper.selectListByUid(keyword, offset, pageSize, uidTypeEnum.getCode(), createUid,
                startTime, endTime, chatTimeEnum.getColumnName());

        List<Integer> historyIdList = list.stream().map(AiChatListResp::getAiChatHistoryId).collect(Collectors.toList());
        if (!historyIdList.isEmpty()) {
            Map<Integer, AiChatHistory> historyMap = aiChatHistoryMapper.selectBatchIds(historyIdList).stream()
                    .collect(Collectors.toMap(AiChatHistory::getId, Function.identity()));
            for (AiChatListResp row : list) {
                row.setHistory(AiChatListResp.convertHistory(keyword, historyMap.get(row.getAiChatHistoryId())));
            }
        }
        return list;
    }

    public boolean updateNameById(Integer chatId, String name) {
        return aiChatMapper.updateNameById(chatId, nameLimit(name), new Date()) > 0;
    }

    public AiChat selectById(Integer chatId) {
        return chatId != null ? aiChatMapper.selectById(chatId) : null;
    }
}
