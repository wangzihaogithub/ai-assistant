package com.github.aiassistant.service.text.chat;

import com.github.aiassistant.dao.AiChatAbortMapper;
import com.github.aiassistant.entity.AiChatAbort;
import com.github.aiassistant.util.AiUtil;
import com.github.aiassistant.util.StringUtils;

import java.util.Collections;
import java.util.Date;
import java.util.Objects;

/**
 * 增删改查-用户停止输出AI回答
 */
//@Service
public class AiChatAbortServiceImpl {
    // @Resource
    private final AiChatAbortMapper aiChatAbortMapper;
    // @Autowired
    private final AiChatHistoryServiceImpl aiChatHistoryService;

    public AiChatAbortServiceImpl(AiChatAbortMapper aiChatAbortMapper, AiChatHistoryServiceImpl aiChatHistoryService) {
        this.aiChatAbortMapper = aiChatAbortMapper;
        this.aiChatHistoryService = aiChatHistoryService;
    }

    /**
     * 用户点击了-停止输出AI回答
     */
    public boolean insert(String beforeText, Integer memoryId, Integer chatId, String userQueryTraceNumber, Integer messageIndex) {
        AiChatAbort abort = new AiChatAbort();
        abort.setCreateTime(new Date());
        abort.setBeforeText(AiUtil.limit(beforeText, 65000, true));
        abort.setAiMemoryId(memoryId);
        abort.setAiChatId(chatId);
        abort.setUserQueryTraceNumber(AiUtil.limit(userQueryTraceNumber, 32, true));
        abort.setMessageIndex(messageIndex);
        if (StringUtils.hasText(userQueryTraceNumber)) {
            String rootAgainUserQueryTraceNumber = aiChatHistoryService.selectRootAgainUserQueryTraceNumberMap(Collections.singletonList(userQueryTraceNumber)).get(userQueryTraceNumber);
            abort.setRootAgainUserQueryTraceNumber(Objects.toString(rootAgainUserQueryTraceNumber, userQueryTraceNumber));
        } else {
            abort.setRootAgainUserQueryTraceNumber("");
        }
        aiChatAbortMapper.insert(abort);
        return true;
    }

}
