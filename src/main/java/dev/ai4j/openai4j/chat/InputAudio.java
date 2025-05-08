package dev.ai4j.openai4j.chat;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class InputAudio {
    /**
     * 音频的 URL 或Base64 Data URL。传入本地文件请参见：输入 Base64 编码的本地文件。
     */
    private String data;
    /**
     * format string（必选）
     * 输入音频的格式，如"mp3"、"wav"等。
     */
    private String format;
    public InputAudio() {}
    public InputAudio(String data, String format) {
        this.data = data;
        this.format = format;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }
}
