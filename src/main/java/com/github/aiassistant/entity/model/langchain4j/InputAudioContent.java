package com.github.aiassistant.entity.model.langchain4j;

import dev.langchain4j.data.audio.Audio;
import dev.langchain4j.data.message.AudioContent;

import java.net.URI;

public class InputAudioContent extends AudioContent {
    /**
     * format string（必选）
     * 输入音频的格式，如"mp3"、"wav"等。
     */
    private final String format;

    public InputAudioContent(URI url, String format) {
        super(url);
        this.format = format;
    }

    public InputAudioContent(String url, String format) {
        super(url);
        this.format = format;
    }

    public InputAudioContent(String base64Data, String mimeType, String format) {
        super(base64Data, mimeType);
        this.format = format;
    }

    public InputAudioContent(Audio audio, String format) {
        super(audio);
        this.format = format;
    }

    public String getFormat() {
        return format;
    }
}
