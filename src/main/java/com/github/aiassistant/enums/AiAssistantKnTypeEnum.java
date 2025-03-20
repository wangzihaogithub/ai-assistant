package com.github.aiassistant.enums;

// import lombok.AllArgsConstructor;

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
    public static final AiAssistantKnTypeEnum rerank = valueOf("rerank");
    // qa("qa", "问答"),
    public static final AiAssistantKnTypeEnum qa = valueOf("qa");
    private final String code;

    private AiAssistantKnTypeEnum(String code) {
        this.code = code;
        CODE_VALUES.put(code, this);
    }

    public static AiAssistantKnTypeEnum valueOf(String code) {
        Objects.requireNonNull(code, "AiAssistantKnTypeEnum#valueOf code cannot be null");
        return CODE_VALUES.computeIfAbsent(code, AiAssistantKnTypeEnum::new);
    }

    public static AiAssistantKnTypeEnum[] values() {
        return CODE_VALUES.values().toArray(new AiAssistantKnTypeEnum[0]);
    }

    public String getCode() {
        return code;
    }
}
