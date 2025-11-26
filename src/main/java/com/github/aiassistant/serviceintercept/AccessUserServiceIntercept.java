package com.github.aiassistant.serviceintercept;

import com.github.aiassistant.entity.model.chat.MemoryIdVO;
import com.github.aiassistant.entity.model.user.AiAccessUserVO;
import com.github.aiassistant.enums.AiChatUidTypeEnum;

import java.io.Serializable;
import java.util.Optional;

public interface AccessUserServiceIntercept extends ServiceIntercept {

    default boolean hasPermission(Serializable chatId, Serializable createUid, AiChatUidTypeEnum uidTypeEnum) {
        return true;
    }

    default MemoryIdVO afterMemoryId(MemoryIdVO memoryIdVO, Serializable chatId, Serializable createUid, AiChatUidTypeEnum uidTypeEnum) {
        return memoryIdVO;
    }

    default <T extends Serializable> T getCurrentUserId() {
//        return StpUtil.getLoginIdAsInt();
        return null;
    }

    default <T extends AiAccessUserVO> T getCurrentUser() {
        return Optional.ofNullable(getCurrentUserId())
                .map(e -> {
                    AiAccessUserVO user = new AiAccessUserVO();
                    user.setId(getCurrentUserId());
                    return (T) user;
                })
                .orElse(null);
    }

}
