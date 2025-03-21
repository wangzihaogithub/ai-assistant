package com.github.aiassistant.service.text.embedding;

import com.github.aiassistant.dao.AiEmbeddingMapper;
import com.github.aiassistant.entity.AiEmbedding;
import com.github.aiassistant.platform.JsonUtil;
import com.github.aiassistant.util.DigestUtils;
import com.github.aiassistant.util.Lists;
import com.github.aiassistant.util.ThrowableUtil;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

public class EmbeddingModelClient {
    // 弱引用根据值触发GC，不能用String触发GC
    private static final Map<float[], String> GLOABL_CACHE_VECTOR_MAP = Collections.synchronizedMap(new WeakHashMap<>(256));
    private final EmbeddingModel model;
    private final LinkedBlockingQueue<EmbeddingCompletableFuture> futureList = new LinkedBlockingQueue<>();
    private final int maxRequestSize;
    private final Executor executor;
    private final AiEmbeddingMapper aiEmbeddingMapper;
    private final String modelName;
    private final int dimensions;
    private final JsonUtil.ObjectWriter objectWriter = JsonUtil.objectWriter();
    private final JsonUtil.ObjectReader objectReader = JsonUtil.objectReader();
    public long cost;
    private int insertPartitionSize = 100;

    public EmbeddingModelClient(EmbeddingModel model, String modelName, int dimensions, Integer maxRequestSize, AiEmbeddingMapper aiEmbeddingMapper, Executor executor) {
        this.model = model;
        this.modelName = modelName;
        this.dimensions = dimensions;
        this.aiEmbeddingMapper = aiEmbeddingMapper;
        this.maxRequestSize = maxRequestSize == null ? 1 : Math.max(1, maxRequestSize);
        this.executor = executor == null ? Runnable::run : executor;
    }

    private static String md5(String keyword) {
        return DigestUtils.md5DigestAsHex(keyword.getBytes(StandardCharsets.UTF_8));
    }

    public CompletableFuture<List<float[]>> addEmbedList(Collection<String> keywordList) {
        if (keywordList == null || keywordList.isEmpty()) {
            return CompletableFuture.completedFuture(Collections.emptyList());
        }
        EmbeddingCompletableFuture future = new EmbeddingCompletableFuture(keywordList);
        futureList.add(future);
        return future;
    }

    public CompletableFuture<float[]> addEmbed(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        return addEmbedList(Collections.singletonList(keyword)).thenApply(e -> e.get(0));
    }

    public List<? extends CompletableFuture<List<float[]>>> embedAllFuture() {
        if (futureList.isEmpty()) {
            return Collections.emptyList();
        }
        ArrayList<EmbeddingCompletableFuture> list = new ArrayList<>(futureList.size());
        futureList.drainTo(list);
        List<String> keywordList = list.stream().flatMap(e -> e.keywordList.stream()).collect(Collectors.toList());
        List<float[]> embeddedList = embedList(keywordList);
        int i = 0;
        for (EmbeddingCompletableFuture future : list) {
            int size = future.keywordList.size();
            future.complete(embeddedList.subList(i, i + size));
            i += size;
        }
        return list;
    }

    private Map<String, float[]> getCacheMap(Collection<String> keywordList) {
        Map<String, float[]> vectorMap = new HashMap<>();
        GLOABL_CACHE_VECTOR_MAP.forEach((key, value) -> {
            if (key == null || value == null) {
                return;
            }
            vectorMap.put(value, key);
        });
        List<String> queryKeywordList = keywordList.stream().filter(vectorMap::containsKey).collect(Collectors.toList());

        if (aiEmbeddingMapper != null && !queryKeywordList.isEmpty()) {
            Map<String, String> keywordMd5Map = new LinkedHashMap<>();
            for (String e : queryKeywordList) {
                keywordMd5Map.put(md5(e), e);
            }
            List<AiEmbedding> embeddingList = aiEmbeddingMapper.selectListByMd5(keywordMd5Map.keySet(), modelName, dimensions);
            for (AiEmbedding aiEmbedding : embeddingList) {
                try {
                    float[] floats = objectReader.readValue(aiEmbedding.getVector(), float[].class);
                    vectorMap.put(keywordMd5Map.get(aiEmbedding.getMd5()), floats);
                } catch (Exception ignored) {
                }
            }
        }
        return vectorMap;
    }

    public List<float[]> embedList(Collection<String> keywordList) {
        if (keywordList == null || keywordList.isEmpty()) {
            return Collections.emptyList();
        }
        long start = System.currentTimeMillis();
        Map<String, float[]> vectorMap = getCacheMap(keywordList);
        List<String> queryKeywordList = keywordList.stream().filter(e -> vectorMap.get(e) == null).collect(Collectors.toList());
        try {
            if (!queryKeywordList.isEmpty()) {
                List<List<String>> partition = Lists.partition(new ArrayList<>(new LinkedHashSet<>(queryKeywordList)), maxRequestSize);
                List<String> partition0 = partition.get(0);
                List<List<String>> partitionN = partition.size() > 1 ? partition.subList(1, partition.size()) : Collections.emptyList();

                List<CompletableFuture<Map<String, float[]>>> futures = new ArrayList<>(partition.size());
                CompletableFuture<Map<String, float[]>> future0 = new CompletableFuture<>();
                for (List<String> keywordN : partitionN) {
                    CompletableFuture<Map<String, float[]>> futureN = new CompletableFuture<>();
                    executor.execute(() -> execute(keywordN, futureN));// 多线程跑
                    futures.add(futureN);
                }
                futures.add(future0);// 省一个线程，当前线程跑第一个

                execute(partition0, future0);// 省一个线程，当前线程跑第一个
                Map<String, float[]> insertVectorMap = new LinkedHashMap<>();
                for (CompletableFuture<Map<String, float[]>> future : futures) {
                    Map<String, float[]> itemVectorMap = future.get();// 多线程跑
                    vectorMap.putAll(itemVectorMap);
                    insertVectorMap.putAll(itemVectorMap);
                }
                executor.execute(() -> putCache(insertVectorMap));
            }
            ArrayList<float[]> list = new ArrayList<>(keywordList.size());
            for (String keyword : keywordList) {
                list.add(vectorMap.get(keyword));
            }
            return list;
        } catch (Exception e) {
            ThrowableUtil.sneakyThrows(e);
            return null;
        } finally {
            cost += System.currentTimeMillis() - start;
        }
    }

    private void putCache(Map<String, float[]> vectorMap) {
        vectorMap.forEach((key, value) -> GLOABL_CACHE_VECTOR_MAP.put(value, key));

        if (aiEmbeddingMapper != null) {
            Date now = new Date();
            List<AiEmbedding> embeddingList = new ArrayList<>();
            vectorMap.forEach((key, value) -> {
                AiEmbedding aiEmbedding = new AiEmbedding();
                aiEmbedding.setKeyword(key);
                aiEmbedding.setMd5(md5(key));
                aiEmbedding.setCreateTime(now);
                aiEmbedding.setModelName(modelName);
                aiEmbedding.setDimensions(dimensions);
                try {
                    aiEmbedding.setVector(objectWriter.writeValueAsString(value));
                } catch (Exception e) {
                    return;
                }
                embeddingList.add(aiEmbedding);
            });
            Lists.partition(embeddingList, insertPartitionSize).forEach(aiEmbeddingMapper::insertIgnoreBatchSomeColumn);
        }
    }

    public int getInsertPartitionSize() {
        return insertPartitionSize;
    }

    public void setInsertPartitionSize(int insertPartitionSize) {
        this.insertPartitionSize = insertPartitionSize;
    }

    private void execute(List<String> keywordList, CompletableFuture<Map<String, float[]>> future) {
        try {
            Response<List<Embedding>> listResponse = model.embedAll(keywordList.stream().map(TextSegment::from).collect(Collectors.toList()));
            List<float[]> vectorList = listResponse.content().stream().map(Embedding::vector).collect(Collectors.toList());

            Map<String, float[]> map = new HashMap<>();
            for (int i = 0, size = keywordList.size(); i < size; i++) {
                map.put(keywordList.get(i), vectorList.get(i));
            }
            future.complete(map);
        } catch (Exception e) {
            future.completeExceptionally(e);
        }
    }

    public float[] embed(String keyword) {
        long start = System.currentTimeMillis();
        try {
            Map<String, float[]> cacheMap = getCacheMap(Collections.singletonList(keyword));
            float[] vector = cacheMap.get(keyword);
            if (vector == null) {
                vector = model.embed(keyword).content().vector();
                putCache(Collections.singletonMap(keyword, vector));
            }
            return vector;
        } finally {
            cost += System.currentTimeMillis() - start;
        }
    }

    @Override
    public String toString() {
        return modelName;
    }

    private static class EmbeddingCompletableFuture extends CompletableFuture<List<float[]>> {
        private final Collection<String> keywordList;

        private EmbeddingCompletableFuture(Collection<String> keywordList) {
            this.keywordList = keywordList;
        }

        @Override
        public String toString() {
            return String.valueOf(keywordList);
        }
    }
}
