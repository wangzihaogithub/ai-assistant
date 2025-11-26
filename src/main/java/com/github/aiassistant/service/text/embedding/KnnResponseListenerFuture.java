package com.github.aiassistant.service.text.embedding;

import com.github.aiassistant.entity.model.chat.KnVO;
import com.github.aiassistant.exception.KnnApiException;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class KnnResponseListenerFuture<T extends KnVO> extends CompletableFuture<List<T>> {
    private final KnnQueryBuilderFuture<T> requestBodyFuture;
    private final CompletableFuture<Request> requestBytesFuture = new CompletableFuture<>();
    private final String indexName;
    private final long createTime = System.currentTimeMillis();

    KnnResponseListenerFuture(KnnQueryBuilderFuture<T> requestBodyFuture, String indexName) {
        this.requestBodyFuture = requestBodyFuture;
        this.indexName = indexName;
    }

    void setRequest(Request request) {
        if (isCancelled()) {
            request.cancel();
        }
        requestBytesFuture.complete(request);
    }

    public long getCreateTime() {
        return createTime;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        boolean b = super.cancel(mayInterruptIfRunning);
        if (b && requestBytesFuture.isDone()) {
            Request request = requestBytesFuture.getNow(null);
            if (request != null) {
                request.cancel();
            }
        }
        return b;
    }

    @Override
    public boolean completeExceptionally(Throwable ex) {
        if (isDone()) {
            return false;
        }
        byte[] requestBody = requestBytesFuture.isCompletedExceptionally() ? null : Optional.ofNullable(requestBytesFuture.getNow(null)).map(e -> e.getRequestBodyBytes()).orElse(null);
        if (!requestBytesFuture.isDone()) {
            requestBytesFuture.completeExceptionally(ex);
        }
        String errorMessage = String.format("knn api error! indexName=%s, cause = %s", indexName, ex);
        return super.completeExceptionally(new KnnApiException(errorMessage, ex, indexName, requestBody));
    }

    public CompletableFuture<byte[]> getRequestBodyBytesFuture() {
        return requestBytesFuture.thenApply(request -> request.getRequestBodyBytes());
    }

    public KnnQueryBuilderFuture<T> getRequestBodyFuture() {
        return requestBodyFuture;
    }

    public byte[] getRequestBodyBytes() {
        if (requestBytesFuture.isCompletedExceptionally()) {
            return null;
        }
        return Optional.ofNullable(requestBytesFuture.getNow(null)).map(e -> e.getRequestBodyBytes()).orElse(null);
    }

    public Map<String, Object> getRequestBody() {
        return requestBodyFuture.isCompletedExceptionally() ? null : requestBodyFuture.getNow(null);
    }

    public String getIndexName() {
        return indexName;
    }

    public interface Request {
        byte[] getRequestBodyBytes();

        void cancel();
    }
}