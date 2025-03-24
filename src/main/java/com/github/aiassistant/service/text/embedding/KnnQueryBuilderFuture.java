package com.github.aiassistant.service.text.embedding;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class KnnQueryBuilderFuture<T> extends CompletableFuture<Map<String, Object>> {
    private final List<Function<List<T>, List<T>>> responseAfterList = new ArrayList<>();
    private final Class<T> type;

    public KnnQueryBuilderFuture(Class<T> type) {
        this.type = type;
    }

    public static <T> KnnQueryBuilderFuture<T> completedFuture(Class<T> type, CompletableFuture<Map<String, Object>> body) {
        KnnQueryBuilderFuture<T> future = new KnnQueryBuilderFuture<>(type);
        body.thenAccept(future::complete)
                .exceptionally(throwable -> {
                    future.completeExceptionally(throwable);
                    return null;
                });
        return future;
    }

    public KnnQueryBuilderFuture<T> addListenerResponseAfter(Function<List<T>, List<T>> responseAfter) {
        responseAfterList.add(responseAfter);
        return this;
    }

    public List<Function<List<T>, List<T>>> getResponseAfterList() {
        return responseAfterList;
    }

    public Class<T> getType() {
        return type;
    }
}
