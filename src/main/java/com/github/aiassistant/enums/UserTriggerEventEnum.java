package com.github.aiassistant.enums;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 用户事件枚举
 */
public class UserTriggerEventEnum<T> {

    private static final Map<String, UserTriggerEventEnum<?>> CODE_VALUES = Collections.synchronizedMap(new LinkedHashMap<>());

    private final String code;
    private final Class<T> payloadClass;

    private UserTriggerEventEnum(String code, Class<T> payloadClass) {
        this.code = code;
        this.payloadClass = payloadClass;
    }

    public static <T> UserTriggerEventEnum<T> valueOf(String code) {
        Objects.requireNonNull(code, "UserTriggerEventEnum#valueOf code cannot be null");
        UserTriggerEventEnum<?> eventEnum = CODE_VALUES.get(code);
        return (UserTriggerEventEnum<T>) eventEnum;
    }

    public static <T> UserTriggerEventEnum<T> create(String code, Class<T> payloadClass) {
        Objects.requireNonNull(code, "UserTriggerEventEnum#create code cannot be null");
        UserTriggerEventEnum<T> exist = valueOf(code);
        if (exist != null) {
            return exist;
        }
        UserTriggerEventEnum<T> value = new UserTriggerEventEnum<>(code, payloadClass);
        CODE_VALUES.put(code, value);
        return value;
    }

    public static UserTriggerEventEnum<?>[] values() {
        return CODE_VALUES.values().toArray(new UserTriggerEventEnum[0]);
    }

    public T cast(Object payload) {
        return payloadClass.cast(payload);
    }

    public String getCode() {
        return code;
    }

    public Class<T> getPayloadClass() {
        return payloadClass;
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
