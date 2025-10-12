package com.github.aiassistant.service.text.embedding;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.github.aiassistant.dao.AiEmbeddingMapper;
import com.github.aiassistant.entity.AiEmbedding;
import com.github.aiassistant.platform.JsonUtil;
import com.github.aiassistant.util.*;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.output.Response;
import okhttp3.*;
import okio.BufferedSink;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Proxy;
import java.nio.charset.Charset;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class EmbeddingModelClient {
    // 弱引用根据值触发GC，不能用String触发GC
    private static final Map<String, Map<float[], String>> GLOABL_CACHE_VECTOR_MAP = new ConcurrentHashMap<>();
    private static final Charset UTF_8 = Charset.forName("UTF-8");
    private static final Map<Integer, float[]> NULL_VECTOR_MAP = new ConcurrentHashMap<>();
    private final Object client;
    private final LinkedBlockingQueue<EmbeddingCompletableFuture> futureList = new LinkedBlockingQueue<>();
    private final int maxRequestSize;
    private final Executor executor;
    private final AiEmbeddingMapper aiEmbeddingMapper;
    private final String modelName;
    private final int dimensions;
    private final JsonUtil.ObjectWriter objectWriter = JsonUtil.objectWriter();
    private final JsonUtil.ObjectReader objectReader = JsonUtil.objectReader();
    private final float[] nullVector;
    private long cost;
    private int insertPartitionSize = 100;

    /**
     * 过时了，去用 {@link EmbeddingModelClient#builder()}
     *
     * @param model             model
     * @param modelName         modelName
     * @param dimensions        dimensions
     * @param maxRequestSize    maxRequestSize
     * @param aiEmbeddingMapper aiEmbeddingMapper
     * @param executor          executor
     * @see EmbeddingModelClient#builder()
     */
    @Deprecated
    public EmbeddingModelClient(EmbeddingModel model,
                                String modelName, int dimensions,
                                Integer maxRequestSize,
                                AiEmbeddingMapper aiEmbeddingMapper, Executor executor) {
        this.client = model;
        this.modelName = modelName;
        this.dimensions = dimensions;
        this.aiEmbeddingMapper = aiEmbeddingMapper;
        this.maxRequestSize = maxRequestSize == null ? 10 : Math.max(1, maxRequestSize);
        this.executor = executor == null ? Runnable::run : executor;
        this.nullVector = NULL_VECTOR_MAP.computeIfAbsent(dimensions, float[]::new);
    }

    private EmbeddingModelClient(
            AiEmbeddingMapper aiEmbeddingMapper,
            Client client,
            String modelName, Integer dimensions,
            Integer maxRequestSize) {
        this.aiEmbeddingMapper = aiEmbeddingMapper;
        this.client = client;
        this.modelName = modelName;
        this.dimensions = dimensions;
        this.maxRequestSize = maxRequestSize == null ? 10 : Math.max(1, maxRequestSize);
        this.executor = Runnable::run;
        this.nullVector = NULL_VECTOR_MAP.computeIfAbsent(dimensions, float[]::new);
    }

    public static Builder builder() {
        return new Builder();
    }

    private static String md5(String keyword) {
        return DigestUtils.md5DigestAsHex(keyword.getBytes(UTF_8));
    }

    private static float[] convert(List<Float> vector) {
        float[] array = new float[vector.size()];
        int i = 0;
        for (Float f : vector) {
            array[i++] = f;
        }
        return array;
    }

    public long getCost() {
        return cost;
    }

    public void resetCost() {
        this.cost = 0;
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
        embedListAsync(keywordList).thenAccept(embeddedList -> {
            int i = 0;
            for (EmbeddingCompletableFuture future : list) {
                int size = future.keywordList.size();
                future.complete(embeddedList.subList(i, i + size));
                i += size;
            }
        }).exceptionally(throwable -> {
            for (EmbeddingCompletableFuture future : list) {
                future.completeExceptionally(throwable);
            }
            return null;
        });
        return list;
    }

    private Map<float[], String> getModelCache() {
        return GLOABL_CACHE_VECTOR_MAP.computeIfAbsent(modelName + "_" + dimensions,
                k -> Collections.synchronizedMap(new WeakHashMap<>(256)));
    }

    private Map<String, float[]> getCacheMap(Collection<String> keywordList) {
        Set<String> queryKeywordList = new HashSet<>(keywordList);
        Map<String, float[]> vectorMap = new HashMap<>();

        Map<float[], String> modelCache = getModelCache();
        modelCache.forEach((key, value) -> {
            if (key == null || value == null) {
                return;
            }
            if (!queryKeywordList.isEmpty() && queryKeywordList.remove(value)) {
                vectorMap.put(value, key);
            }
        });

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
        try {
            return embedListAsync(keywordList).get();
        } catch (Exception ex) {
            ThrowableUtil.sneakyThrows(ex);
            return Collections.emptyList();
        }
    }

    public CompletableFuture<List<float[]>> embedListAsync(Collection<String> keywordList) {
        if (keywordList == null || keywordList.isEmpty()) {
            return CompletableFuture.completedFuture(Collections.emptyList());
        }
        long start = System.currentTimeMillis();
        Map<String, float[]> vectorMap = getCacheMap(keywordList);
        List<String> queryKeywordList = keywordList.stream().filter(e -> vectorMap.get(e) == null).collect(Collectors.toList());
        try {
            if (queryKeywordList.isEmpty()) {
                ArrayList<float[]> list = new ArrayList<>(keywordList.size());
                for (String keyword : keywordList) {
                    list.add(vectorMap.get(keyword));
                }
                return CompletableFuture.completedFuture(list);
            }
            List<List<String>> partition = Lists.partition(new ArrayList<>(new LinkedHashSet<>(queryKeywordList)), maxRequestSize);
            List<CompletableFuture<Map<String, float[]>>> futures = new ArrayList<>(partition.size());
            for (List<String> keywordN : partition) {
                CompletableFuture<Map<String, float[]>> futureN = new CompletableFuture<>();
                executor.execute(() -> execute(keywordN, futureN));// 多线程跑
                futures.add(futureN);
            }
            return FutureUtil.allOf(futures)
                    .thenApply(rowList -> {
                        Map<String, float[]> insertMap = new LinkedHashMap<>();
                        for (Map<String, float[]> row : rowList) {
                            vectorMap.putAll(row);
                            insertMap.putAll(row);
                        }
                        executor.execute(() -> putCache(insertMap));
                        ArrayList<float[]> list = new ArrayList<>(keywordList.size());
                        for (String keyword : keywordList) {
                            list.add(vectorMap.get(keyword));
                        }
                        return list;
                    });
        } catch (Throwable e) {
            CompletableFuture<List<float[]>> future = new CompletableFuture<>();
            future.completeExceptionally(e);
            return future;
        } finally {
            cost += System.currentTimeMillis() - start;
        }
    }

    private void putCache(Map<String, float[]> vectorMap) {
        Map<float[], String> modelCache = getModelCache();
        vectorMap.forEach((key, value) -> modelCache.put(value, key));

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
            if (keywordList.isEmpty()) {
                future.complete(new HashMap<>());
                return;
            }
            if (this.client instanceof OpenAiEmbeddingModel) {
                OpenAiEmbeddingModel client = ((OpenAiEmbeddingModel) this.client);
                Map<String, float[]> map = new HashMap<>(keywordList.size());
                Response<List<Embedding>> listResponse = client.embedAll(keywordList.stream().map(TextSegment::from).collect(Collectors.toList()));
                List<float[]> vectorList = listResponse.content().stream().map(Embedding::vector).collect(Collectors.toList());
                for (int i = 0, size = keywordList.size(); i < size; i++) {
                    map.put(keywordList.get(i), vectorList.get(i));
                }
                future.complete(map);
            } else if (this.client instanceof Client) {
                Client client = ((Client) this.client);
                client.embedding(keywordList, modelName, dimensions, objectWriter, objectReader)
                        .thenAccept(future::complete)
                        .exceptionally(throwable -> {
                            future.completeExceptionally(throwable);
                            return null;
                        });
            } else {
                future.completeExceptionally(new IllegalStateException("embedding client IllegalState!"));
            }
        } catch (Throwable e) {
            future.completeExceptionally(e);
        }
    }

    public float[] embed(String keyword) {
        long start = System.currentTimeMillis();
        try {
            float[] vector;
            if (StringUtils.hasText(keyword)) {
                Map<String, float[]> cacheMap = getCacheMap(Collections.singletonList(keyword));
                vector = cacheMap.get(keyword);
                if (vector == null) {
                    if (this.client instanceof OpenAiEmbeddingModel) {
                        OpenAiEmbeddingModel client = ((OpenAiEmbeddingModel) this.client);
                        vector = client.embed(keyword).content().vector();
                    } else if (this.client instanceof Client) {
                        Client client = ((Client) this.client);
                        CompletableFuture<Map<String, float[]>> future = client.embedding(Collections.singletonList(keyword), modelName, dimensions, objectWriter, objectReader);
                        Map<String, float[]> map = future.get();
                        Iterator<float[]> iterator = map.values().iterator();
                        return iterator.hasNext() ? iterator.next() : nullVector;
                    } else {
                        throw new IllegalStateException("embedding client IllegalState!");
                    }
                    putCache(Collections.singletonMap(keyword, vector));
                }
            } else {
                vector = nullVector;
            }
            return vector;
        } catch (Throwable e) {
            ThrowableUtil.sneakyThrows(e);
            return nullVector;
        } finally {
            cost += System.currentTimeMillis() - start;
        }
    }

    @Override
    public String toString() {
        return modelName + "[" + dimensions + "]";
    }

    public static class Factory {
        final Client client;
        final AiEmbeddingMapper aiEmbeddingMapper;
        final String baseUrl;
        final String modelName;
        final Integer dimensions;
        final Integer maxRequestSize;

        private Factory(
                AiEmbeddingMapper aiEmbeddingMapper,
                String baseUrl, String apiKey,
                String modelName, Integer dimensions,
                Duration timeout,
                Proxy proxy,
                Map<String, String> customHeaders,
                Integer maxRequestSize) {
            timeout = timeout != null ? timeout : Duration.ofSeconds(60L);
            this.aiEmbeddingMapper = aiEmbeddingMapper;

            OkHttpClient.Builder okHttpClientBuilder = new OkHttpClient.Builder()
                    .callTimeout(timeout)
                    .connectTimeout(timeout)
                    .readTimeout(timeout)
                    .writeTimeout(timeout);

            Map<String, String> headers = new HashMap<>();
            headers.put("User-Agent", "langchain4j-openai");
            if (customHeaders != null) {
                headers.putAll(customHeaders);
            }
            if (apiKey != null && !apiKey.isEmpty()) {
                headers.put("Authorization", "Bearer " + apiKey);
            }
            headers.put("api-key", apiKey);
            okHttpClientBuilder.addInterceptor(chain -> {
                Request.Builder builder = chain.request().newBuilder();
                headers.forEach(builder::addHeader);
                return chain.proceed(builder.build());
            });
            if (proxy != null) {
                okHttpClientBuilder.proxy(proxy);
            }
            okHttpClientBuilder.dispatcher(new Dispatcher(new ThreadPoolExecutor(0, Integer.MAX_VALUE, 60, TimeUnit.SECONDS,
                    new SynchronousQueue<>(), runnable -> {
                Thread result = new Thread(runnable);
                result.setName("OkHttp ai-embedding-" + result.getId());
                result.setDaemon(false);
                return result;
            })));
            this.client = new Client(baseUrl, okHttpClientBuilder.build());
            this.baseUrl = baseUrl;
            this.modelName = modelName;
            this.dimensions = dimensions;
            this.maxRequestSize = maxRequestSize == null ? 10 : Math.max(1, maxRequestSize);
        }

        public EmbeddingModelClient get() {
            return new EmbeddingModelClient(aiEmbeddingMapper, client, modelName, dimensions, maxRequestSize);
        }
    }

    private static class Client {
        private static final MediaType mediaType = MediaType.parse("application/json");

        final String baseUrl;
        final OkHttpClient client;

        private Client(String baseUrl, OkHttpClient client) {
            this.baseUrl = baseUrl + "/embeddings";
            this.client = client;
        }

        public CompletableFuture<Map<String, float[]>> embedding(List<String> keywordList,
                                                                 String modelName, int dimensions,
                                                                 JsonUtil.ObjectWriter objectWriter,
                                                                 JsonUtil.ObjectReader objectReader) {
            Request request = new Request.Builder()
                    .url(baseUrl)
                    .post(new RequestBody() {
                        @Override
                        public MediaType contentType() {
                            return mediaType;
                        }

                        @Override
                        public void writeTo(BufferedSink sink) throws IOException {
                            EmbeddingRequest embeddingRequest = new EmbeddingRequest();
                            embeddingRequest.setModel(modelName);
                            embeddingRequest.setDimensions(dimensions);
                            embeddingRequest.setInput(keywordList);
                            objectWriter.writeValue(new OutputStream() {
                                @Override
                                public void write(int b) throws IOException {
                                    sink.write(new byte[]{(byte) b}, 0, 1);
                                }

                                @Override
                                public void write(byte[] b, int off, int len) throws IOException {
                                    sink.write(b, off, len);
                                }

                                @Override
                                public void flush() throws IOException {
                                    sink.flush();
                                }
                            }, embeddingRequest);
                        }
                    })
                    .build();
            // 异步调用
            Call call = client.newCall(request);
            CompletableFuture<Map<String, float[]>> future = new CompletableFuture<>();
            call.enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    future.completeExceptionally(e);
                }

                @Override
                public void onResponse(Call call, okhttp3.Response response) throws IOException {
                    try {
                        ResponseBody body = response.body();
                        if (response.isSuccessful() && body != null) {
                            Map<String, float[]> map = new HashMap<>(keywordList.size());
                            EmbeddingResponse embeddingResponse = objectReader.readValue(body.byteStream(), EmbeddingResponse.class);
                            List<Embedding> vectorList = embeddingResponse.data;
                            for (int i = 0, size = keywordList.size(); i < size; i++) {
                                float[] vector = vectorList.get(i).embedding;
                                map.put(keywordList.get(i), vector);
                            }
                            future.complete(map);
                        } else {
                            String string = body == null ? "" : body.string();
                            future.completeExceptionally(new IOException("embedding fail! code=" + response.code() + ", body=" + string + ", headers=" + response.headers()));
                        }
                    } catch (Throwable e) {
                        future.completeExceptionally(e);
                    }
                }
            });
            return future;
        }

        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
        public static class EmbeddingRequest {
            private String model;
            private List<String> input;
            private Integer dimensions;

            public String getModel() {
                return model;
            }

            public void setModel(String model) {
                this.model = model;
            }

            public List<String> getInput() {
                return input;
            }

            public void setInput(List<String> input) {
                this.input = input;
            }

            public Integer getDimensions() {
                return dimensions;
            }

            public void setDimensions(Integer dimensions) {
                this.dimensions = dimensions;
            }
        }

        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
        public static class EmbeddingResponse {
            private List<Embedding> data;

            public List<Embedding> getData() {
                return data;
            }

            public void setData(List<Embedding> data) {
                this.data = data;
            }
        }

        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
        public static class Embedding {
            private float[] embedding;
            private Integer index;

            public float[] getEmbedding() {
                return embedding;
            }

            public void setEmbedding(float[] embedding) {
                this.embedding = embedding;
            }

            public Integer getIndex() {
                return index;
            }

            public void setIndex(Integer index) {
                this.index = index;
            }
        }
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

    public static class Builder {
        private AiEmbeddingMapper aiEmbeddingMapper;
        private String baseUrl;
        private String apiKey;
        private String modelName;
        private Integer dimensions;
        private Duration timeout;
        private Proxy proxy;
        private Map<String, String> customHeaders;

        private Integer maxRequestSize;

        public Builder() {
        }

        public Builder aiEmbeddingMapper(AiEmbeddingMapper aiEmbeddingMapper) {
            this.aiEmbeddingMapper = aiEmbeddingMapper;
            return this;
        }

        public Builder modelName(String modelName) {
            this.modelName = modelName;
            return this;
        }

        public Builder baseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        public Builder apiKey(String apiKey) {
            this.apiKey = apiKey;
            return this;
        }

        public Builder dimensions(Integer dimensions) {
            this.dimensions = dimensions;
            return this;
        }

        public Builder maxRequestSize(Integer maxRequestSize) {
            this.maxRequestSize = maxRequestSize;
            return this;
        }

        public Builder timeout(Duration timeout) {
            this.timeout = timeout;
            return this;
        }

        public Builder proxy(Proxy proxy) {
            this.proxy = proxy;
            return this;
        }

        public Builder customHeaders(Map<String, String> customHeaders) {
            this.customHeaders = customHeaders;
            return this;
        }

        public Factory build() {
            return new Factory(this.aiEmbeddingMapper, this.baseUrl, this.apiKey, this.modelName, this.dimensions, this.timeout, this.proxy, this.customHeaders, this.maxRequestSize);
        }

        @Override
        public String toString() {
            return "EmbeddingModelClient.Builder(baseUrl=" + this.baseUrl + ", apiKey=" + this.apiKey + ", modelName=" + this.modelName + ", dimensions=" + this.dimensions + ", timeout=" + this.timeout + ", proxy=" + this.proxy + ", customHeaders=" + this.customHeaders + ")";
        }
    }
}
