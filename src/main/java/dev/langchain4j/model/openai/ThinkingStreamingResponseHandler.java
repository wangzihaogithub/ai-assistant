package dev.langchain4j.model.openai;

import dev.langchain4j.model.StreamingResponseHandler;

/**
 * 思考模型
 *
 * @param <T> T
 */
public interface ThinkingStreamingResponseHandler<T> extends StreamingResponseHandler<T> {

    /**
     * 思考模型
     *
     * @param thinkingToken 思考内容
     */
    void onThinkingToken(String thinkingToken);

}
