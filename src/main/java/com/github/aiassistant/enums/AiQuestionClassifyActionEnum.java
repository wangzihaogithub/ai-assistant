package com.github.aiassistant.enums;

// import lombok.AllArgsConstructor;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 问题分类控制的动作
 */
public enum AiQuestionClassifyActionEnum {
    qa("qa", "问答库"),
    jdlw("jdlw", "简单联网"),
    wtcj("wtcj", "问题拆解"),
    dclw("dclw", "多层联网"),
    lwdd("lwdd", "联网兜底"),
    wfhd("wfhd", "无法回答");

    private final String code;
    private final String name;

    AiQuestionClassifyActionEnum(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public static Collection<AiQuestionClassifyActionEnum> parse(String code) {
        if (code == null || code.isEmpty()) {
            return Collections.emptyList();
        }
        String[] split = code.split(",");
        Collection<AiQuestionClassifyActionEnum> collect = Arrays.stream(split).map(AiQuestionClassifyActionEnum::getByCode).filter(Objects::nonNull).collect(Collectors.toList());
        if (collect.isEmpty()) {
            return Collections.emptyList();
        }
        return EnumSet.copyOf(collect);
    }

    public static AiQuestionClassifyActionEnum getByCode(String code) {
        return Arrays.stream(values())
                .filter(e -> e.code.equals(code))
                .findFirst()
                .orElse(null);
    }

    public String getName() {
        return name;
    }

    public String getCode() {
        return code;
    }
}
