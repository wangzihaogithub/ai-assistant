package com.github.aiassistant.enums;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 知识库类型枚举（qa=问答，majorjob=专业，job=岗位）
 */
public class AiAssistantKnTypeEnum {
    //    qa("qa", "问答"),
//    majorjob("majorjob", "岗位专业"),
//    majorLike("majorLike", "专业相似"),
//    job("job", "岗位"),
//    rerank("rerank", "RAG重新排序"),
//    retrieverIndustrySalary("retrieverIndustrySalary", "召回工具(行业薪酬)"),
//    retrieverEmployees("retrieverEmployees", "召回工具(员工信息)"),
//    retrieverCnwy("retrieverCnwy", "召回工具(菜鸟无忧)");
//
    private static final Map<String, AiAssistantKnTypeEnum> CODE_VALUES = Collections.synchronizedMap(new LinkedHashMap<>());
    // rerank("rerank", "RAG重新排序"),
    public static final AiAssistantKnTypeEnum rerank = create("rerank");
    // qa("qa", "问答"),
    public static final AiAssistantKnTypeEnum qa = create("qa");
    private final String code;

    private AiAssistantKnTypeEnum(String code) {
        this.code = code;
    }

    public static AiAssistantKnTypeEnum valueOf(String code) {
        Objects.requireNonNull(code, "AiAssistantKnTypeEnum#valueOf code cannot be null");
        return CODE_VALUES.get(code);
    }

    public static AiAssistantKnTypeEnum create(String code) {
        Objects.requireNonNull(code, "AiAssistantKnTypeEnum#create code cannot be null");
        AiAssistantKnTypeEnum exist = valueOf(code);
        if (exist != null) {
            return exist;
        }
        AiAssistantKnTypeEnum value = new AiAssistantKnTypeEnum(code);
        CODE_VALUES.put(code, value);
        return value;
    }

    public static AiAssistantKnTypeEnum[] values() {
        return CODE_VALUES.values().toArray(new AiAssistantKnTypeEnum[0]);
    }

    public String getCode() {
        return code;
    }

    @Override
    public String toString() {
        return code;
    }
}
