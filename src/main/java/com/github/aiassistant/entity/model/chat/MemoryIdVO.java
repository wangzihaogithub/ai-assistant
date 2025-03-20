package com.github.aiassistant.entity.model.chat;

import com.github.aiassistant.entity.*;
import com.github.aiassistant.enums.AiAssistantKnTypeEnum;
import com.github.aiassistant.util.AiUtil;
import com.github.aiassistant.service.text.tools.Tools;

import java.util.*;

public class MemoryIdVO {
    /**
     * 聊天
     */
    private AiChat aiChat;
    /**
     * 智能体
     */
    private AiAssistant aiAssistant;
    /**
     * jsonschema类型的定义与提示词
     */
    private List<AiJsonschema> jsonschemaList;
    /**
     * 智能体记忆状态字段的定义与提示词
     */
    private List<AiAssistantMstate> mstateList;
    /**
     * 少样本学习
     */
    private List<AiAssistantFewshot> fewshotList;
    /**
     * 知识库
     */
    private Map<String, List<AiAssistantKn>> assistantKnMap;
    private List<Tools.ToolMethod> toolMethodList;

    public AiChat getAiChat() {
        return aiChat;
    }

    public void setAiChat(AiChat aiChat) {
        this.aiChat = aiChat;
    }

    public AiAssistant getAiAssistant() {
        return aiAssistant;
    }

    public void setAiAssistant(AiAssistant aiAssistant) {
        this.aiAssistant = aiAssistant;
    }

    public List<AiJsonschema> getJsonschemaList() {
        return jsonschemaList;
    }

    public void setJsonschemaList(List<AiJsonschema> jsonschemaList) {
        this.jsonschemaList = jsonschemaList;
    }

    public List<AiAssistantMstate> getMstateList() {
        return mstateList;
    }

    public void setMstateList(List<AiAssistantMstate> mstateList) {
        this.mstateList = mstateList;
    }

    public List<AiAssistantFewshot> getFewshotList() {
        return fewshotList;
    }

    public void setFewshotList(List<AiAssistantFewshot> fewshotList) {
        this.fewshotList = fewshotList;
    }

    public Map<String, List<AiAssistantKn>> getAssistantKnMap() {
        return assistantKnMap;
    }

    public void setAssistantKnMap(Map<String, List<AiAssistantKn>> assistantKnMap) {
        this.assistantKnMap = assistantKnMap;
    }

    public List<Tools.ToolMethod> getToolMethodList() {
        return toolMethodList;
    }

    public void setToolMethodList(List<Tools.ToolMethod> toolMethodList) {
        this.toolMethodList = toolMethodList;
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

    public boolean isEnableJsonschema(String jsonSchemaEnum) {
        return jsonschemaList != null && jsonschemaList.stream()
                .anyMatch(e -> Objects.equals(jsonSchemaEnum, e.getJsonSchemaEnum()) && Boolean.TRUE.equals(e.getEnableFlag()));
    }

    public AiJsonschema getJsonschema(String jsonSchemaEnum) {
        if(jsonschemaList == null || jsonschemaList.isEmpty()){
            return null;
        }
        return jsonschemaList.stream()
                .filter(e -> Objects.equals(jsonSchemaEnum, e.getJsonSchemaEnum()) && Boolean.TRUE.equals(e.getEnableFlag()))
                .findFirst()
                .orElse(null);
    }

    public String getMstateJsonPrompt() {
        if (mstateList == null || mstateList.isEmpty()) {
            return null;
        }
        Map<String, String> map = new LinkedHashMap<>();
        for (AiAssistantMstate mstate : mstateList) {
            map.put(mstate.getStateKey(), mstate.getPromptText());
        }
        return AiUtil.toJsonString(map);
    }

    @Override
    public String toString() {
        return aiChat == null ? super.toString() : aiChat.getId() + "#" + aiChat.getName();
    }
}
