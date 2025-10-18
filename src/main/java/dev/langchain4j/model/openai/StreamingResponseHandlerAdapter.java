package dev.langchain4j.model.openai;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.StreamingResponseHandler;
import okhttp3.Call;
import okhttp3.Response;

public interface StreamingResponseHandlerAdapter extends ThinkingStreamingResponseHandler, AudioStreamingResponseHandler, StreamingResponseHandler<AiMessage>, HttpResponseHandler {
    @Override
    default void onThinkingToken(String thinkingToken) {

    }

    @Override
    default void onAudio(AudioChunk audioChunk) {

    }

    @Override
    default void onHttpResponse(Call call, Response response) {

    }
}
