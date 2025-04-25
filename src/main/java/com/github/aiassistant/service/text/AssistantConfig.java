package com.github.aiassistant.service.text;

import com.github.aiassistant.entity.AiAssistant;
import com.github.aiassistant.entity.AiQuestionClassifyAssistant;

public interface AssistantConfig {
    public static AssistantConfig select(AiAssistant assistant, AiQuestionClassifyAssistant classifyAssistant) {
        return classifyAssistant != null ? classifyAssistant : assistant;
    }

    String getTableName();

    String getName();

    public String getSystemPromptText();

    public String getAiToolIds();

    public String getAiJsonschemaIds();

    public Integer getMaxMemoryTokens();

    public Integer getMaxMemoryRounds();

    public Integer getMaxCompletionTokens();

    public String getChatApiKey();

    public String getChatBaseUrl();

    public String getChatModelName();

    public Double getTemperature();

    String getKnPromptText();

    String getMstatePromptText();
}
