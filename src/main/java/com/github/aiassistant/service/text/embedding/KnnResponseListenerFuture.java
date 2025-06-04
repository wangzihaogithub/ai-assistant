package com.github.aiassistant.service.text.embedding;

import com.github.aiassistant.entity.model.chat.KnVO;
import com.github.aiassistant.exception.KnnApiException;
import com.github.aiassistant.platform.JsonUtil;
import com.github.aiassistant.util.BeanUtil;
import org.elasticsearch.client.Cancellable;
import org.elasticsearch.client.Response;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;

public class KnnResponseListenerFuture<T extends KnVO> extends CompletableFuture<List<T>> {
    private final KnnQueryBuilderFuture<T> requestBodyFuture;
    private final CompletableFuture<Request> requestBytesFuture = new CompletableFuture<>();
    private final double minScore;
    private final Double knTop1Score;
    private final String indexName;
    private final long createTime = System.currentTimeMillis();
    private Response response;

    KnnResponseListenerFuture(double minScore,
                              Double knTop1Score, KnnQueryBuilderFuture<T> requestBodyFuture, String indexName) {
        this.minScore = minScore;
        this.knTop1Score = knTop1Score;
        this.requestBodyFuture = requestBodyFuture;
        this.indexName = indexName;
    }

    void setRequest(Cancellable cancellable,
                    byte[] requestBody) {
        if (isCancelled()) {
            cancellable.cancel();
        }
        requestBytesFuture.complete(new Request(cancellable, requestBody));
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
                request.cancellable.cancel();
            }
        }
        return b;
    }

    void onSuccess(Response response) {
        this.response = response;
        try {
            List<T> result = map(response);
            complete(result);
        } catch (Exception e) {
            completeExceptionally(e);
        }
    }

    private List<T> map(Response response) throws IOException {
        // response
        Map content = JsonUtil.objectReader().readValue(response.getEntity().getContent(), Map.class);
        Map hits = (Map) content.get("hits");
        List<Map> hitsList = (List<Map>) hits.get("hits");
        List<T> list = new ArrayList<>();

        Class<T> type = requestBodyFuture.getType();
        for (Map hit : hitsList) {
            double score = ((Number) hit.get("_score")).doubleValue();
            T source = BeanUtil.toBean((Map<String, Object>) hit.get("_source"), type);
            source.setId(Objects.toString(hit.get("_id"), null));
            source.setScore(score);
            source.setIndexName(Objects.toString(hit.get("_index"), null));
            list.add(source);
        }

        List<T> l = list;
        List<BiFunction<List<T>, Map, List<T>>> afterList = requestBodyFuture.getResponseAfterList();
        if (afterList != null) {
            for (BiFunction<List<T>, Map, List<T>> after : afterList) {
                l = after.apply(l, content);
            }
        }
        return filter(l);
    }

    private List<T> filter(List<T> list) {
        List<T> result = new ArrayList<>(list.size());
        for (T hit : list) {
            Double score = hit.getScore();
            if (minScore == 0 || score >= minScore) {
                if (knTop1Score != null && score >= knTop1Score) {
                    return new ArrayList<>(Collections.singletonList(hit));
                }
                result.add(hit);
            }
        }
        return result;
    }

    @Override
    public boolean completeExceptionally(Throwable ex) {
        if (isDone()) {
            return false;
        }
        byte[] requestBody = requestBytesFuture.isCompletedExceptionally() ? null : Optional.ofNullable(requestBytesFuture.getNow(null)).map(e -> e.requestBody).orElse(null);
        if (!requestBytesFuture.isDone()) {
            requestBytesFuture.completeExceptionally(ex);
        }
        String errorMessage = String.format("knn api error! indexName=%s, cause = %s", indexName, ex);
        return super.completeExceptionally(new KnnApiException(errorMessage, ex, indexName, requestBody));
    }

    public CompletableFuture<byte[]> getRequestBodyBytesFuture() {
        return requestBytesFuture.thenApply(request -> request.requestBody);
    }

    public KnnQueryBuilderFuture<T> getRequestBodyFuture() {
        return requestBodyFuture;
    }

    public byte[] getRequestBodyBytes() {
        if (requestBytesFuture.isCompletedExceptionally()) {
            return null;
        }
        return Optional.ofNullable(requestBytesFuture.getNow(null)).map(e -> e.requestBody).orElse(null);
    }

    public Map<String, Object> getRequestBody() {
        return requestBodyFuture.isCompletedExceptionally() ? null : requestBodyFuture.getNow(null);
    }

    public String getIndexName() {
        return indexName;
    }

    private static class Request {
        final Cancellable cancellable;
        final byte[] requestBody;

        private Request(Cancellable cancellable, byte[] requestBody) {
            this.cancellable = cancellable;
            this.requestBody = requestBody;
        }
    }
}