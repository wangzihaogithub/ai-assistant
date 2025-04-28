package com.github.aiassistant.entity.model.chat;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;

public class AiModel {
    public final String baseUrl;
    public final String modelName;
    public final ChatLanguageModel model;
    public final StreamingChatLanguageModel streaming;

    public AiModel(String baseUrl, String modelName, ChatLanguageModel model, StreamingChatLanguageModel streaming) {
        this.baseUrl = baseUrl;
        this.modelName = modelName;
        this.model = model;
        this.streaming = streaming;
    }

    public boolean isSupportChineseToolName() {
        return !modelName.startsWith("deepseek");
    }

    @Override
    public String toString() {
        return modelName;
    }
}