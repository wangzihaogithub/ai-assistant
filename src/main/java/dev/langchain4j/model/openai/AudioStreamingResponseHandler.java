package dev.langchain4j.model.openai;

/**
 * 模型层的语音回复（多模态）
 */
public interface AudioStreamingResponseHandler {

    /**
     * 语音回复（多模态）
     *
     * @param audioChunk 部分语音回复块
     */
    void onAudio(AudioChunk audioChunk);

}
