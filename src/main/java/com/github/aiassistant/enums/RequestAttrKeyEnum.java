package com.github.aiassistant.enums;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 用户自定义的请求属性枚举(明确请求属性有哪些，以及属性的类型)
 */
public class RequestAttrKeyEnum<T> {

    private static final Map<String, RequestAttrKeyEnum<?>> CODE_VALUES = Collections.synchronizedMap(new LinkedHashMap<>());

    private final String code;
    private final Class<T> valueClass;

    private RequestAttrKeyEnum(String code, Class<T> valueClass) {
        this.code = code;
        this.valueClass = valueClass;
    }

    public static <T> RequestAttrKeyEnum<T> valueOf(String code) {
        Objects.requireNonNull(code, "AiRequestAttrKeyEnum#valueOf code cannot be null");
        RequestAttrKeyEnum<?> eventEnum = CODE_VALUES.get(code);
        return (RequestAttrKeyEnum<T>) eventEnum;
    }

    public static <T> RequestAttrKeyEnum<T> create(String code, Class<T> payloadClass) {
        Objects.requireNonNull(code, "AiRequestAttrKeyEnum#create code cannot be null");
        RequestAttrKeyEnum<T> exist = valueOf(code);
        if (exist != null) {
            return exist;
        }
        RequestAttrKeyEnum<T> value = new RequestAttrKeyEnum<>(code, payloadClass);
        CODE_VALUES.put(code, value);
        return value;
    }

    public static RequestAttrKeyEnum<?>[] values() {
        return CODE_VALUES.values().toArray(new RequestAttrKeyEnum[0]);
    }

    public T cast(Object payload) {
        return valueClass.cast(payload);
    }

    public String getCode() {
        return code;
    }

    public Class<T> getValueClass() {
        return valueClass;
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
