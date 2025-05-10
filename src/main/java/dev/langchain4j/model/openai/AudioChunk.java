package dev.langchain4j.model.openai;

import dev.ai4j.openai4j.chat.Delta;

import java.util.Base64;

/**
 * 语音回复（多模态）
 */
public class AudioChunk {
    private final Delta.Audio audio;
    private volatile byte[] audioBytes;

    AudioChunk(Delta.Audio audio) {
        this.audio = audio;
    }

    public String getBase64() {
        return audio.getData();
    }

    public String getTranscript() {
        return audio.getTranscript();
    }

    public byte[] getBytes() {
        if (audioBytes == null) {
            synchronized (this) {
                if (audioBytes == null) {
                    String audioBase64 = this.audio.getData();
                    if (audioBase64 != null && !audioBase64.isEmpty()) {
                        audioBytes = Base64.getDecoder().decode(audioBase64);
                    }
                }
            }
        }
        return audioBytes;
    }

    @Override
    public String toString() {
        return audio.getData();
    }
}
