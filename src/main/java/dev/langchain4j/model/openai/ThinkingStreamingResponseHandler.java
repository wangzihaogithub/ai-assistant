package dev.langchain4j.model.openai;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.output.Response;

/**
 * 模型层的思考
 */
public interface ThinkingStreamingResponseHandler {
    /**
     * 开始思考
     */
    default void onStartThinking() {

    }

    /**
     * 思考模型
     *
     * @param thinkingToken 思考内容
     */
    void onThinkingToken(String thinkingToken);

    /**
     * 思考完成
     *
     * @param thinkingResponse thinkingResponse
     */
    default void onCompleteThinking(Response<AiMessage> thinkingResponse) {

    }
}
