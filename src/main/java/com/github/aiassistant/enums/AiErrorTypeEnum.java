package com.github.aiassistant.enums;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

public class AiErrorTypeEnum {
    public static final String onTokenReadTimeout = "onTokenReadTimeout";
    private static final Map<String, AiErrorTypeEnum> CODE_VALUES = Collections.synchronizedMap(new LinkedHashMap<>());
    public static final AiErrorTypeEnum limit_requests = create("limit_requests", "限流", "当前使用人数较多，建议等待片刻后重新提问。", e -> {
        return e.contains("limit_requests");
    });
    public static final AiErrorTypeEnum data_inspection_failed = create("data_inspection_failed", "违规", "根据相关规定暂无法回答，我们可以聊聊其他话题吗？", e -> {
        return e.contains("data_inspection_failed");
    });
    public static final AiErrorTypeEnum token_read_timeout = create("token_read_timeout", "读取超时", "当前问题处理超时，建议您稍后重新尝试提问。", e -> {
        return e.contains(AiErrorTypeEnum.onTokenReadTimeout);
    });
    public static final AiErrorTypeEnum unknown_error = create("unknown_error", "未知异常", "当前问题处理超时，建议您稍后重新尝试提问。", e -> {
        return false;
    });
    //    student("student", "学生"),
//    sys_user("sys_user", "员工");
    private final String code;
    private final String name;
    private final String messageText;
    private final Predicate<String> test;

    protected AiErrorTypeEnum(String code, String name, String messageText, Predicate<String> test) {
        this.code = code;
        this.name = name;
        this.messageText = messageText;
        this.test = test;
    }

    public static AiErrorTypeEnum parseErrorType(String errorString) {
        for (AiErrorTypeEnum value : values()) {
            if (value != unknown_error && value.test(errorString)) {
                return value;
            }
        }
        return unknown_error;
    }

    public static AiErrorTypeEnum valueOf(String code) {
        Objects.requireNonNull(code, "AiErrorTypeEnum#valueOf code cannot be null");
        return CODE_VALUES.get(code);
    }

    public static AiErrorTypeEnum create(String code, String name, String messageText, Predicate<String> test) {
        Objects.requireNonNull(code, "AiErrorTypeEnum#create code cannot be null");
        AiErrorTypeEnum exist = valueOf(code);
        if (exist != null) {
            return exist;
        }
        AiErrorTypeEnum value = new AiErrorTypeEnum(code, name, messageText, test);
        CODE_VALUES.put(code, value);
        return value;
    }

    public static AiErrorTypeEnum create(AiErrorTypeEnum code) {
        Objects.requireNonNull(code, "AiErrorTypeEnum#create code cannot be null");
        CODE_VALUES.put(code.code, code);
        return code;
    }

    public static AiErrorTypeEnum[] values() {
        return CODE_VALUES.values().toArray(new AiErrorTypeEnum[0]);
    }

    public String getCode() {
        return code;
    }

    public Predicate<String> getTest() {
        return test;
    }

    public boolean test(String errorString) {
        return test != null && test.test(errorString);
    }

    public String getName() {
        return name;
    }

    public String getMessageText() {
        return messageText;
    }

}
