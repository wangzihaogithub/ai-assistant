package dev.langchain4j.model.openai;

/**
 * 模型层的语音回复（多模态）
 */
public interface AudioStreamingResponseHandler {
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
     * https://bailian.console.aliyun.com/?tab=api#/api/?type=model&url=https%3A%2F%2Fhelp.aliyun.com%2Fdocument_detail%2F2867839.html
     *
     * @param audioChunk 部分语音回复块
     */
    void onAudio(AudioChunk audioChunk);

}
