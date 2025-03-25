package com.github.aiassistant.enums;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class AiWebSearchSourceEnum {
    private static final Map<String, AiWebSearchSourceEnum> CODE_VALUES = Collections.synchronizedMap(new LinkedHashMap<>());
    public static final AiWebSearchSourceEnum LlmTextApiService = create("LlmTextApiService");
    public static final AiWebSearchSourceEnum ActingJsonSchema = create("ActingJsonSchema");
    private final String code;

    protected AiWebSearchSourceEnum(String code) {
        this.code = code;
    }

    public static AiWebSearchSourceEnum valueOf(String code) {
        Objects.requireNonNull(code, "AiWebSearchSourceEnum#valueOf code cannot be null");
        return CODE_VALUES.get(code);
    }

    public static AiWebSearchSourceEnum create(String code) {
        Objects.requireNonNull(code, "AiWebSearchSourceEnum#create code cannot be null");
        AiWebSearchSourceEnum exist = valueOf(code);
        if (exist != null) {
            return exist;
        }
        AiWebSearchSourceEnum value = new AiWebSearchSourceEnum(code);
        CODE_VALUES.put(code, value);
        return value;
    }

    public static AiWebSearchSourceEnum create(AiWebSearchSourceEnum code) {
        Objects.requireNonNull(code, "AiWebSearchSourceEnum#create code cannot be null");
        CODE_VALUES.put(code.code, code);
        return code;
    }

    public static AiWebSearchSourceEnum[] values() {
        return CODE_VALUES.values().toArray(new AiWebSearchSourceEnum[0]);
    }

    public String getCode() {
        return code;
    }

}
