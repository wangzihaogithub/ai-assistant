package com.github.aiassistant.entity.model.chat;

import java.util.LinkedHashMap;
import java.util.Map;

public class MStateVO {
    private Map<String, String> knownState;
    private Map<String, String> unknownState;

    public MStateVO(Map<String, String> knownState, Map<String, String> unknownState) {
        this.knownState = knownState;
        this.unknownState = unknownState;
    }

    public MStateVO() {
    }

    public static MStateVO empty() {
        return new MStateVO(new LinkedHashMap<>(), new LinkedHashMap<>());
    }

    public boolean isEmpty() {
        return (knownState == null || knownState.isEmpty()) && (unknownState == null || unknownState.isEmpty());
    }

    public Map<String, String> getKnownState() {
        return knownState;
    }

    public void setKnownState(Map<String, String> knownState) {
        this.knownState = knownState;
    }

    public Map<String, String> getUnknownState() {
        return unknownState;
    }

    public void setUnknownState(Map<String, String> unknownState) {
        this.unknownState = unknownState;
    }
}
