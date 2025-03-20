package com.github.aiassistant.service.jsonschema;

import com.github.aiassistant.util.AiUtil;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

import java.util.concurrent.CompletableFuture;

/**
 * 是否等待AI
 */
@FunctionalInterface
public interface WhetherWaitingForAiJsonSchema {

    @UserMessage("请你判断<aimessage>这个xml标签中的内容，这段内容是\"AI\"给\"用户\"的回答，你需要从实际用户体验角度，判断出这段回答是否包含以下情况，如果都满足返回true，否则返回false。\n" +
            "1.\"AI\"即将为用户查询或推荐数据，并且内容中未严格明确提供具体招聘岗位名称，并且\"AI\"没有向\"用户\"咨询后续意见。\n" +
            "<aimessage>{{aiMessage}}</aimessage>")
    TokenStream isWaitingForAi(@V("aiMessage") String aiMessage);

    default CompletableFuture<Boolean> future(String aiMessage) {
        return AiUtil.toFutureBoolean(isWaitingForAi(aiMessage));
    }

}
