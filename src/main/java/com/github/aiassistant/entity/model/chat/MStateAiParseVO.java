package com.github.aiassistant.entity.model.chat;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class MStateAiParseVO {
    private CompletableFuture<Map<String, Object>> knownState;
    private CompletableFuture<Map<String, Object>> unknownState;

    public MStateAiParseVO() {
    }

    public MStateAiParseVO(CompletableFuture<Map<String, Object>> knownState, CompletableFuture<Map<String, Object>> unknownState) {
        this.knownState = knownState;
        this.unknownState = unknownState;
    }

    public CompletableFuture<Map<String, Object>> getKnownState() {
        return knownState;
    }

    public CompletableFuture<Map<String, Object>> getUnknownState() {
        return unknownState;
    }
}
