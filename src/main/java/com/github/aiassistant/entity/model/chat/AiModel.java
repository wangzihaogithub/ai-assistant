package com.github.aiassistant.entity.model.chat;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;

public class AiModel {
    public final String baseUrl;
    public final String modelName;
    public final ChatLanguageModel model;
    public final OpenAiStreamingChatModel streaming;

    public AiModel(String baseUrl, String modelName, ChatLanguageModel model, OpenAiStreamingChatModel streaming) {
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