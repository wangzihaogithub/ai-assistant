package dev.ai4j.openai4j.chat;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum ContentType {

    @JsonProperty("text")
    TEXT,
    @JsonProperty("image_url")
    IMAGE_URL,

    /**
     * ai-assistant扩展字段
     * 向Qwen-Omni 模型输入音频时需要设为"input_audio"。
     */
    @JsonProperty("input_audio")
    INPUT_AUDIO,
    /**
     * ai-assistant扩展字段
     * 向Qwen-VL 模型、QVQ模型或Qwen-Omni 模型输入图片列表形式的视频时需要设为"video"。
     */
    @JsonProperty("video")
    VIDEO,
    /**
     * ai-assistant扩展字段
     * 向Qwen-VL 模型、QVQ模型或Qwen-Omni 模型输入视频文件时需要设为"video_url"。
     */
    @JsonProperty("video_url")
    VIDEO_URL
}
