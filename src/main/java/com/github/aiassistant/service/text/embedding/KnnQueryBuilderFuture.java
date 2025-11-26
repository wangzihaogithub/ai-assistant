package com.github.aiassistant.service.text.embedding;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Consumer;

public class KnnQueryBuilderFuture<T> extends CompletableFuture<Map<String, Object>> implements Cloneable {
    private final Class<T> responseType;
    private final List<BiFunction<List<T>, Map, List<T>>> responseAfterList;
    private final List<Consumer<Map<String, Object>>> completeBeforeList;

    public KnnQueryBuilderFuture(Class<T> responseType) {
        this(responseType, new ArrayList<>(), new ArrayList<>());
    }

    public KnnQueryBuilderFuture(Class<T> responseType,
                                 List<BiFunction<List<T>, Map, List<T>>> responseAfterList,
                                 List<Consumer<Map<String, Object>>> completeBeforeList) {
        this.responseType = responseType;
        this.completeBeforeList = completeBeforeList;
        this.responseAfterList = responseAfterList;
    }

    public static <T> KnnQueryBuilderFuture<T> completedFuture(Class<T> responseType, CompletableFuture<Map<String, Object>> requestBody) {
        KnnQueryBuilderFuture<T> future = new KnnQueryBuilderFuture<>(responseType);
        requestBody.thenAccept(future::complete)
                .exceptionally(throwable -> {
                    future.completeExceptionally(throwable);
                    return null;
                });
        return future;
    }

    @Override
    public boolean complete(Map<String, Object> value) {
        if (!isDone()) {
            for (Consumer<Map<String, Object>> mapConsumer : completeBeforeList) {
                mapConsumer.accept(value);
            }
        }
        return super.complete(value);
    }

    @Override
    public void obtrudeValue(Map<String, Object> value) {
        if (!isDone()) {
            for (Consumer<Map<String, Object>> mapConsumer : completeBeforeList) {
                mapConsumer.accept(value);
            }
        }
        super.obtrudeValue(value);
    }

    public KnnQueryBuilderFuture<T> fork() {
        return new KnnQueryBuilderFuture<>(responseType, responseAfterList, completeBeforeList);
    }

    public KnnQueryBuilderFuture<T> addListenerCompleteBefore(Consumer<Map<String, Object>> completeBefore) {
        completeBeforeList.add(completeBefore);
        return this;
    }

    public KnnQueryBuilderFuture<T> addListenerResponseAfter(BiFunction<List<T>, Map, List<T>> responseAfter) {
        responseAfterList.add(responseAfter);
        return this;
    }

    public List<Consumer<Map<String, Object>>> getCompleteBeforeList() {
        return completeBeforeList;
    }

    public List<BiFunction<List<T>, Map, List<T>>> getResponseAfterList() {
        return responseAfterList;
    }

    public Class<T> getResponseType() {
        return responseType;
    }
}
