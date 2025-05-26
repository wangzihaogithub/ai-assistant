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

    public boolean isDone() {
        return (knownState == null || knownState.isDone()) && (unknownState == null || unknownState.isDone());
    }

    public CompletableFuture<Void> future() {
        CompletableFuture[] futures = new CompletableFuture[]{
                knownState == null ? CompletableFuture.completedFuture(null) : knownState,
                unknownState == null ? CompletableFuture.completedFuture(null) : unknownState
        };
        return CompletableFuture.allOf(futures);
    }

    public CompletableFuture<Map<String, Object>> getKnownState() {
        return knownState;
    }

    public CompletableFuture<Map<String, Object>> getUnknownState() {
        return unknownState;
    }
}
