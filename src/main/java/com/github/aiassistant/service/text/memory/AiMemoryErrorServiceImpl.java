package com.github.aiassistant.service.text.memory;

import com.github.aiassistant.dao.AiMemoryErrorMapper;
import com.github.aiassistant.entity.AiMemoryError;
import com.github.aiassistant.entity.model.chat.MemoryIdVO;
import com.github.aiassistant.entity.model.chat.RequestTrace;
import com.github.aiassistant.entity.model.user.AiAccessUserVO;
import com.github.aiassistant.enums.AiErrorTypeEnum;
import com.github.aiassistant.service.text.chat.AiChatHistoryServiceImpl;
import com.github.aiassistant.util.StringUtils;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.Date;
import java.util.Objects;

/**
 * 增删改查-异常
 */
public class AiMemoryErrorServiceImpl {
    // @Resource
    private final AiMemoryErrorMapper aiMemoryErrorMapper;
    // @Autowired
    private final AiChatHistoryServiceImpl aiChatHistoryService;

    public AiMemoryErrorServiceImpl(AiMemoryErrorMapper aiMemoryErrorMapper, AiChatHistoryServiceImpl aiChatHistoryService) {
        this.aiMemoryErrorMapper = aiMemoryErrorMapper;
        this.aiChatHistoryService = aiChatHistoryService;
    }

    /**
     * 内部异常
     *
     * @param throwable        异常
     * @param baseMessageIndex 消息下标
     * @param addMessageCount  本次多少
     * @param generateCount    生成数量
     * @param requestTrace     请求
     * @return AiMemoryError
     */
    public AiMemoryError insertByInner(Throwable throwable, int baseMessageIndex, int addMessageCount, int generateCount, RequestTrace<MemoryIdVO, AiAccessUserVO> requestTrace) {
        StringWriter buf = new StringWriter();
        throwable.printStackTrace(new PrintWriter(buf));

        String userQueryTraceNumber = requestTrace.getUserQueryTraceNumber();

        AiMemoryError error = new AiMemoryError();
        error.setAiChatId(requestTrace.getMemoryId().getChatId());
        error.setMemoryId(requestTrace.getMemoryId().getMemoryId());
        error.setErrorClassName(StringUtils.left(throwable.getClass().getName(), 128, true));
        error.setErrorMessage(StringUtils.left(buf.toString(), 65000, true));
        error.setUserQueryTraceNumber(userQueryTraceNumber);
        error.setMessageCount(requestTrace.getMessageSize());

        error.setBaseMessageIndex(baseMessageIndex);
        error.setAddMessageCount(addMessageCount);
        error.setGenerateCount(generateCount);
        error.setCreateTime(new Date());
        error.setSessionTime(requestTrace.getCreateTime());

        AiErrorTypeEnum errorTypeEnum = AiErrorTypeEnum.parseErrorType(throwable);
        error.setErrorType(errorTypeEnum.getCode());
        error.setMessageText(StringUtils.left(errorTypeEnum.getMessageText(), 512, true));
        error.setAttachmentJson("");
        if (StringUtils.hasText(userQueryTraceNumber)) {
            String rootAgainUserQueryTraceNumber = aiChatHistoryService.selectRootAgainUserQueryTraceNumberMap(Collections.singletonList(userQueryTraceNumber)).get(userQueryTraceNumber);
            error.setRootAgainUserQueryTraceNumber(Objects.toString(rootAgainUserQueryTraceNumber, userQueryTraceNumber));
        } else {
            error.setRootAgainUserQueryTraceNumber("");
        }
        aiMemoryErrorMapper.insert(error);
        return error;
    }

    /**
     * 前端异常
     *
     * @param memoryId             记忆ID
     * @param userQueryTraceNumber 问题编号
     * @param messageIndex         消息下标
     * @param errorClassName       错误
     * @param errorMessage         错误
     * @param timestamp            时间
     * @param errorType            错误类型
     * @param messageText          消息
     * @param attachmentJson       附加
     * @return AiMemoryError
     */
    public AiMemoryError insertByOnerror(MemoryIdVO memoryId,
                                         String userQueryTraceNumber, Integer messageIndex,
                                         String errorClassName,
                                         String errorMessage,
                                         Long timestamp,
                                         String errorType,
                                         String messageText,
                                         String attachmentJson) {
        Date now = new Date();

        AiMemoryError error = new AiMemoryError();
        error.setAiChatId(memoryId.getChatId());
        error.setMemoryId(memoryId.getMemoryId());
        error.setErrorClassName(StringUtils.left(errorClassName, 128, true));
        error.setErrorMessage(StringUtils.left(errorMessage, 65000, true));
        error.setUserQueryTraceNumber(StringUtils.left(userQueryTraceNumber, 32, true));
        error.setMessageCount(0);

        error.setBaseMessageIndex(messageIndex == null ? -1 : messageIndex);
        error.setAddMessageCount(-1);
        error.setGenerateCount(-1);
        error.setCreateTime(now);
        error.setSessionTime(timestamp != null ? new Timestamp(timestamp) : now);

        error.setErrorType(StringUtils.left(errorType, 128, true));
        error.setMessageText(StringUtils.left(messageText, 512, true));
        error.setAttachmentJson(StringUtils.left(attachmentJson, 65000, true));
        if (StringUtils.hasText(userQueryTraceNumber)) {
            String rootAgainUserQueryTraceNumber = aiChatHistoryService.selectRootAgainUserQueryTraceNumberMap(Collections.singletonList(userQueryTraceNumber)).get(userQueryTraceNumber);
            error.setRootAgainUserQueryTraceNumber(Objects.toString(rootAgainUserQueryTraceNumber, userQueryTraceNumber));
        } else {
            error.setRootAgainUserQueryTraceNumber("");
        }
        aiMemoryErrorMapper.insert(error);
        return error;
    }

}
