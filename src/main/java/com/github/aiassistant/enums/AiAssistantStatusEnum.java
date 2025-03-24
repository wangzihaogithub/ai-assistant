package com.github.aiassistant.enums;

import java.util.Arrays;

public enum AiAssistantStatusEnum {
    //启用
    enable(1),
    //停用
    disable(0);

    private final Integer code;

    AiAssistantStatusEnum(Integer code) {
        this.code = code;
    }

    public static AiAssistantStatusEnum getByCode(Integer code) {
        return Arrays.stream(values())
                .filter(e -> e.code.equals(code))
                .findFirst()
                .orElse(null);
    }

    public Integer getCode() {
        return code;
    }
}
