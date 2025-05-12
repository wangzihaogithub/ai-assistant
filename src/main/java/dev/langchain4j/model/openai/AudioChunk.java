package dev.langchain4j.model.openai;

import dev.ai4j.openai4j.chat.Delta;

import java.util.Base64;

/**
 * 语音回复（多模态）
 * wav_bytes = base64.b64decode(audio_string)
 * audio_np = np.frombuffer(wav_bytes, dtype=np.int16)
 * sf.write("audio_assistant_py.wav", audio_np, samplerate=24000)
 * <p>
 * 输出音频的音色与文件格式（只支持设定为"wav"）通过audio参数来配置，
 * 如：audio={"voice": "Cherry", "format": "wav"}，
 * 其中商业版模型voice参数可选值为：["Cherry", "Serena", "Ethan", "Chelsie"]，
 * 开源版模型voice参数可选值为：["Ethan", "Chelsie"]。
 * https://bailian.console.aliyun.com/?tab=api#/api/?type=model
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
