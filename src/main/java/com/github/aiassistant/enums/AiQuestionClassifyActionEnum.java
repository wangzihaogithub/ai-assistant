package com.github.aiassistant.enums;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 问题分类控制的动作
 */
public class AiQuestionClassifyActionEnum {
    private static final Map<String, AiQuestionClassifyActionEnum> CODE_VALUES = Collections.synchronizedMap(new LinkedHashMap<>());
    public static final AiQuestionClassifyActionEnum qa = create("qa", "问答库", true);
    public static final AiQuestionClassifyActionEnum jdlw = create("jdlw", "简单联网", true);
    public static final AiQuestionClassifyActionEnum wtcj = create("wtcj", "问题拆解", true);
    public static final AiQuestionClassifyActionEnum dclw = create("dclw", "多层联网", true);
    public static final AiQuestionClassifyActionEnum lwdd = create("lwdd", "联网兜底", true);
    public static final AiQuestionClassifyActionEnum wfhd = create("wfhd", "无法回答", false);
    private final String code;
    private final String name;
    private final boolean defaultEnable;

    protected AiQuestionClassifyActionEnum(String code, String name, boolean defaultEnable) {
        this.code = code;
        this.name = name;
        this.defaultEnable = defaultEnable;
    }

    public static AiQuestionClassifyActionEnum valueOf(String code) {
        Objects.requireNonNull(code, "AiQuestionClassifyActionEnum#valueOf code cannot be null");
        return CODE_VALUES.get(code);
    }

    public static AiQuestionClassifyActionEnum create(String code, String name, boolean defaultEnable) {
        Objects.requireNonNull(code, "AiQuestionClassifyActionEnum#create code cannot be null");
        AiQuestionClassifyActionEnum exist = valueOf(code);
        if (exist != null) {
            return exist;
        }
        AiQuestionClassifyActionEnum value = new AiQuestionClassifyActionEnum(code, name, defaultEnable);
        CODE_VALUES.put(code, value);
        return value;
    }

    public static AiQuestionClassifyActionEnum create(AiQuestionClassifyActionEnum code) {
        Objects.requireNonNull(code, "AiQuestionClassifyActionEnum#create code cannot be null");
        CODE_VALUES.put(code.code, code);
        return code;
    }

    public static AiQuestionClassifyActionEnum[] values() {
        return CODE_VALUES.values().toArray(new AiQuestionClassifyActionEnum[0]);
    }

    public static Collection<AiQuestionClassifyActionEnum> parse(String code) {
        if (code == null || code.isEmpty()) {
            return Collections.emptyList();
        }
        String[] split = code.split(",");
        return Arrays.stream(split)
                .map(AiQuestionClassifyActionEnum::valueOf)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public String getName() {
        return name;
    }

    public String getCode() {
        return code;
    }

    public boolean isDefaultEnable() {
        return defaultEnable;
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
