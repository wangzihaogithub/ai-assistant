package com.github.aiassistant.entity.model.chat;

import com.github.aiassistant.entity.AiAssistant;
import com.github.aiassistant.entity.AiAssistantKn;
import com.github.aiassistant.entity.AiChat;
import com.github.aiassistant.enums.AiAssistantKnTypeEnum;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class MemoryIdVO implements Cloneable {
    /**
     * 聊天
     */
    private AiChat aiChat;
    /**
     * 智能体
     */
    private AiAssistant aiAssistant;
    /**
     * 知识库
     */
    private Map<String, List<AiAssistantKn>> assistantKnMap;

    @Override
    public MemoryIdVO clone() {
        try {
            MemoryIdVO clone = (MemoryIdVO) super.clone();
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new IllegalStateException(e.toString(), e);
        }
    }

    public AiChat getAiChat() {
        return aiChat;
    }

    public void setAiChat(AiChat aiChat) {
        this.aiChat = aiChat;
    }

    public String getAiAssistantId() {
        return aiChat.getAssistantId();
    }

    public AiAssistant getAiAssistant() {
        return aiAssistant;
    }

    public void setAiAssistant(AiAssistant aiAssistant) {
        this.aiAssistant = aiAssistant;
    }

    public Map<String, List<AiAssistantKn>> getAssistantKnMap() {
        return assistantKnMap;
    }

    public void setAssistantKnMap(Map<String, List<AiAssistantKn>> assistantKnMap) {
        this.assistantKnMap = assistantKnMap;
    }

    public AiAssistantKn getAssistantKn(AiAssistantKnTypeEnum name) {
        List<AiAssistantKn> list = getAssistantKnList(name);
        return list == null || list.isEmpty() ? null : list.get(0);
    }

    public List<AiAssistantKn> getAssistantKnList(AiAssistantKnTypeEnum name) {
        return assistantKnMap.getOrDefault(name.getCode(), Collections.emptyList());
    }

    public <T> T indexAt(T[] arrays) {
        return arrays[getChatId() % arrays.length];
    }

    public Integer getMemoryId() {
        return aiChat.getAiMemoryId();
    }

    public Integer getChatId() {
        return aiChat.getId();
    }


    @Override
    public String toString() {
        return aiChat == null ? super.toString() : aiChat.getId() + "#" + aiChat.getName();
    }
}
