package com.github.aiassistant.enums;

import com.github.aiassistant.entity.AiChat;

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class AiChatUidTypeEnum {
    private static final Map<String, AiChatUidTypeEnum> CODE_VALUES = Collections.synchronizedMap(new LinkedHashMap<>());
    //    student("student", "学生"),
//    sys_user("sys_user", "员工");
    private final String code;

    protected AiChatUidTypeEnum(String code) {
        this.code = code;
    }

    public static AiChatUidTypeEnum valueOf(String code) {
        Objects.requireNonNull(code, "AiChatUidTypeEnum#valueOf code cannot be null");
        return CODE_VALUES.get(code);
    }

    public static AiChatUidTypeEnum create(String code) {
        Objects.requireNonNull(code, "AiChatUidTypeEnum#create code cannot be null");
        AiChatUidTypeEnum exist = valueOf(code);
        if (exist != null) {
            return exist;
        }
        AiChatUidTypeEnum value = new AiChatUidTypeEnum(code);
        CODE_VALUES.put(code, value);
        return value;
    }

    public static AiChatUidTypeEnum create(AiChatUidTypeEnum code) {
        Objects.requireNonNull(code, "AiChatUidTypeEnum#create code cannot be null");
        AiChatUidTypeEnum exist = valueOf(code.code);
        if (exist != null) {
            return exist;
        }
        CODE_VALUES.put(code.code, code);
        return code;
    }

    public static AiChatUidTypeEnum[] values() {
        return CODE_VALUES.values().toArray(new AiChatUidTypeEnum[0]);
    }

    public String getCode() {
        return code;
    }

    public boolean hasPermission(AiChat aiChat, Serializable uid) {
        if (aiChat == null) {
            return false;
        }
        return code.equals(aiChat.getUidType()) && Objects.equals(Objects.toString(uid, null), Objects.toString(aiChat.getCreateUid(), null));
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public String toString() {
        return code;
    }
}
