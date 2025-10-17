package com.github.aiassistant.service.text;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.StreamingResponseHandler;
import dev.langchain4j.model.openai.AudioChunk;
import dev.langchain4j.model.openai.AudioStreamingResponseHandler;
import dev.langchain4j.model.openai.ThinkingStreamingResponseHandler;

public interface StreamingResponseHandlerAdapter extends ThinkingStreamingResponseHandler, AudioStreamingResponseHandler, StreamingResponseHandler<AiMessage> {
    @Override
    default void onThinkingToken(String thinkingToken) {

    }

    @Override
    default void onAudio(AudioChunk audioChunk) {

    }

}
