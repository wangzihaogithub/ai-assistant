package dev.ai4j.openai4j.chat;

import java.util.Objects;

/**
 * 输出音频的音色与文件格式（只支持设定为"wav"）通过audio参数来配置，
 * 如：audio={"voice": "Cherry", "format": "wav"}，
 * 其中商业版模型voice参数可选值为：["Cherry", "Serena", "Ethan", "Chelsie"]，
 * 开源版模型voice参数可选值为：["Ethan", "Chelsie"]。
 * https://bailian.console.aliyun.com/?tab=api#/api/?type=model&url=https%3A%2F%2Fhelp.aliyun.com%2Fdocument_detail%2F2867839.html
 */
public class Audio {
    /**
     * 音色
     * Cherry
     */
    private String voice;
    /**
     * wav
     */
    private String format;

    public Audio() {
    }

    public Audio(String voice, String format) {
        this.voice = voice;
        this.format = format;
    }

    public String getVoice() {
        return voice;
    }

    public void setVoice(String voice) {
        this.voice = voice;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Audio)) return false;
        Audio audio = (Audio) o;
        return Objects.equals(voice, audio.voice) && Objects.equals(format, audio.format);
    }

    @Override
    public int hashCode() {
        return Objects.hash(voice, format);
    }

    @Override
    public String toString() {
        return "Audio{" +
                "voice='" + voice + '\'' +
                ", format='" + format + '\'' +
                '}';
    }
}
