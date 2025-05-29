package com.github.aiassistant.service.text.embedding;

import com.github.aiassistant.entity.model.chat.KnVO;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ListKnnResponseListenerFuture<T extends KnVO> extends CompletableFuture<List<T>> {
    private final List<KnnResponseListenerFuture<T>> list;

    ListKnnResponseListenerFuture(List<KnnResponseListenerFuture<T>> list) {
        this.list = list;
    }

}