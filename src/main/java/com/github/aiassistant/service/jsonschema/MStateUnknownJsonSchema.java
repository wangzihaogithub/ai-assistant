package com.github.aiassistant.service.jsonschema;

import com.github.aiassistant.util.AiUtil;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 记忆状态
 */
@FunctionalInterface
public interface MStateUnknownJsonSchema {

    @SystemMessage("你使用用户提供的" +
            "memorystring标签中的聊天记录，" +
            "userquery标签中是用户最后的提问，" +
            "mstate标签中是你上一次提取的关键信息结果，" +
            "你需要根据上一次提取的关键信息结果与用户最后的提问，计算出最新的关键信息数据" +
            "你在必要时可以参考聊天记录。" +
            "不要解释，必须以正确json格式返回。")
    @UserMessage("<memorystring>{{chat.historyMessage}}</memorystring>" +
            "<userquery>{{chat.query}}</userquery>" +
            "<mstate>{{mstate}}</mstate>")
    TokenStream parse(@V("mstate") String mstate);

    default CompletableFuture<Map<String, Object>> future(String mstate) {
        TokenStream stream = parse(mstate);
        CompletableFuture future = AiUtil.toFutureJson(stream, LinkedHashMap.class, getClass());
        return future;
    }
}
