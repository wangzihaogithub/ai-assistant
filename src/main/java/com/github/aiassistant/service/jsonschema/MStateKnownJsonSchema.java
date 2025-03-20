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
public interface MStateKnownJsonSchema {

    @SystemMessage("你使用用户提供的" +
            "memorystring标签中的聊天记录，" +
            "userquery标签中是用户最后的提问，" +
            "mstate标签中是你上一次提取的关键信息结果，" +
            "你需要根据上一次提取的关键信息结果与用户最后的提问，计算出最新的关键信息数据" +
            "你在必要时可以参考聊天记录。" +
            "字段提取的需求在mprompt标签内。" +
            "严格遵循mprompt标签内的字段名，不要解释，必须以正确json格式返回。")
    @UserMessage("<memorystring>{{chat.historyMessage}}</memorystring>" +
            "<userquery>{{chat.query}}</userquery>" +
            "<mstate>{{mstate}}</mstate>" +
            "<mprompt>{{mstateJsonPrompt}}</mprompt>")
    TokenStream parse(@V("mstateJsonPrompt") String mstateJsonPrompt, @V("mstate") String mstate);

    default CompletableFuture<Map<String, Object>> future(String mstateJsonPrompt, String mstate) {
        CompletableFuture future = AiUtil.toFutureJson(parse(mstateJsonPrompt, mstate), LinkedHashMap.class);
        return future;
    }
}
