package com.github.aiassistant.entity.model.chat;

import com.github.aiassistant.entity.AiAssistant;
import com.github.aiassistant.entity.AiAssistantKn;
import com.github.aiassistant.entity.AiChat;
import com.github.aiassistant.enums.AiAssistantKnTypeEnum;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class MemoryIdVO implements Cloneable {
    public static final MemoryIdVO NULL = new MemoryIdVO() {
        @Override
        public void setAiAssistant(AiAssistant aiAssistant) {
        }

        @Override
        public void setAssistantKnMap(Map<String, List<AiAssistantKn>> assistantKnMap) {
        }

        @Override
        public void setAiChat(AiChat aiChat) {

        }

        @Override
        public String toString() {
            return "NULLMemoryIdVO";
        }
    };
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
        if (aiChat == null) {
            return null;
        }
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
        if (assistantKnMap == null) {
            return null;
        }
        return assistantKnMap.getOrDefault(name.getCode(), Collections.emptyList());
    }

    public <T> T indexAt(T[] arrays) {
        if (aiChat == null) {
            return arrays[0];
        }
        return arrays[getChatId() % arrays.length];
    }

    public Integer getMemoryId() {
        if (aiChat == null) {
            return null;
        }
        return aiChat.getAiMemoryId();
    }

    public Integer getChatId() {
        if (aiChat == null) {
            return null;
        }
        return aiChat.getId();
    }

    @Override
    public String toString() {
        return aiChat == null ? super.toString() : aiChat.getId() + "#" + aiChat.getName();
    }
}
