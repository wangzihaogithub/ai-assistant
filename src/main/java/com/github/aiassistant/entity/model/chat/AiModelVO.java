package com.github.aiassistant.entity.model.chat;

import dev.langchain4j.model.openai.OpenAiChatClient;

import java.io.Serializable;

public class AiModelVO implements Serializable {
    public final String baseUrl;
    public final String modelName;
    public final OpenAiChatClient streaming;

    public AiModelVO(String baseUrl, String modelName,  OpenAiChatClient streaming) {
        this.baseUrl = baseUrl;
        this.modelName = modelName;
        this.streaming = streaming;
    }

    /**
     * 是否支持中文工具名称（deepseek仅支持英文名称）
     *
     * @return true=支持
     */
    public boolean isSupportChineseToolName() {
        return !modelName.startsWith("deepseek");
    }

    @Override
    public String toString() {
        return modelName;
    }
}