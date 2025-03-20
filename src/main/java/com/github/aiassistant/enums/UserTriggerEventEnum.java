package com.github.aiassistant.enums;

// import lombok.AllArgsConstructor;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 用户事件枚举
 */
public class UserTriggerEventEnum {

    private static final Map<String, UserTriggerEventEnum> CODE_VALUES = Collections.synchronizedMap(new LinkedHashMap<>());

    private final String code;

    private UserTriggerEventEnum(String code) {
        this.code = code;
        CODE_VALUES.put(code, this);
    }

    public static UserTriggerEventEnum valueOf(String code) {
        Objects.requireNonNull(code, "UserTriggerEventEnum#valueOf code cannot be null");
        return CODE_VALUES.computeIfAbsent(code, UserTriggerEventEnum::new);
    }

    public static UserTriggerEventEnum[] values() {
        return CODE_VALUES.values().toArray(new UserTriggerEventEnum[0]);
    }

    public String getCode() {
        return code;
    }
}
