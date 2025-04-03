package com.github.aiassistant.enums;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 聊天来源枚举（pc=pc端创建的，wxmini=微信小程序）
 */
public class AiChatSourceEnum {
    private static final Map<String, AiChatSourceEnum> CODE_VALUES = Collections.synchronizedMap(new LinkedHashMap<>());
    //    pc("pc", "pc端"),
//    wxmini("wxmini", "微信小程序");
    private final String code;

    protected AiChatSourceEnum(String code) {
        this.code = code;
    }

    public static AiChatSourceEnum valueOf(String code) {
        Objects.requireNonNull(code, "AiChatSourceEnum#valueOf code cannot be null");
        return CODE_VALUES.get(code);
    }

    public static AiChatSourceEnum create(String code) {
        Objects.requireNonNull(code, "AiChatSourceEnum#create code cannot be null");
        AiChatSourceEnum exist = valueOf(code);
        if (exist != null) {
            return exist;
        }
        AiChatSourceEnum value = new AiChatSourceEnum(code);
        CODE_VALUES.put(code, value);
        return value;
    }

    public static AiChatSourceEnum create(AiChatSourceEnum code) {
        Objects.requireNonNull(code, "AiChatSourceEnum#create code cannot be null");
        AiChatSourceEnum exist = valueOf(code.code);
        if (exist != null) {
            return exist;
        }
        CODE_VALUES.put(code.code, code);
        return code;
    }

    public static AiChatSourceEnum[] values() {
        return CODE_VALUES.values().toArray(new AiChatSourceEnum[0]);
    }

    public String getCode() {
        return code;
    }

}
