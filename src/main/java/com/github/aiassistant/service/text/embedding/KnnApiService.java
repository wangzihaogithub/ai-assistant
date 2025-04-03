package com.github.aiassistant.service.text.embedding;

import com.github.aiassistant.dao.AiEmbeddingMapper;
import com.github.aiassistant.entity.AiAssistantKn;
import com.github.aiassistant.entity.model.chat.KnVO;
import com.github.aiassistant.platform.JsonUtil;
import com.github.aiassistant.util.AiUtil;
import com.github.aiassistant.util.BeanUtil;
import com.github.aiassistant.util.StringUtils;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import org.apache.http.client.entity.EntityBuilder;
import org.apache.http.entity.ContentType;
import org.elasticsearch.client.*;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 向量模型服务
 */
// @Component
public class KnnApiService {
    /**
     * 向量存储
     */
    private final RestClient embeddingStore;
    /**
     * 聊天模型
     */
    private final JsonUtil.ObjectWriter objectWriter = JsonUtil.objectWriter();
    /**
     * 向量模型
     */
    private final Map<String, OpenAiEmbeddingModel[]> modelMap = new ConcurrentHashMap<>();
    /**
     * 每个智能体的向量化模型并发数量
     */
    private final int concurrentEmbeddingModelCount;
    private final Executor executor;
    private final AiEmbeddingMapper aiEmbeddingMapper;
    /**
     * 取模轮训下标
     */
    private int modelModIndex = 0;

    public KnnApiService(RestClient embeddingStore) {
        this(embeddingStore, null, 10);
    }

    public KnnApiService(RestClient embeddingStore, AiEmbeddingMapper aiEmbeddingMapper) {
        this(embeddingStore, aiEmbeddingMapper, 10);
    }

    public KnnApiService(RestClient embeddingStore, AiEmbeddingMapper aiEmbeddingMapper, int concurrentEmbeddingModelCount) {
        ThreadPoolExecutor poolExecutor = new ThreadPoolExecutor(
                concurrentEmbeddingModelCount, concurrentEmbeddingModelCount,
                60, TimeUnit.SECONDS,
                new SynchronousQueue<>(), target -> {
            Thread thread = new Thread(target);
            thread.setName("Ai-Embedding-" + thread.getId());
            thread.setDaemon(true);
            return thread;
        }, new ThreadPoolExecutor.CallerRunsPolicy());
        poolExecutor.allowCoreThreadTimeOut(true);
        this.concurrentEmbeddingModelCount = concurrentEmbeddingModelCount;
        this.executor = poolExecutor;
        this.aiEmbeddingMapper = aiEmbeddingMapper;
//        RestClientBuilder builder = RestClient.builder(HttpHost.create(config.getElasticsearch().getServerUrl()));
//        if (StringUtils.hasText(config.getElasticsearch().getApiKey())) {
//            builder.setDefaultHeaders(new Header[]{
//                    new BasicHeader("Authorization", "ApiKey " + config.getElasticsearch().getApiKey())
//            });
//        }
//        settingIO(builder);
        this.embeddingStore = embeddingStore;
    }

//    public static void settingIO(RestClientBuilder builder) {
//        int concurrentRequest = 50;
//        int socketTimeout = 120_000;
//        builder.setRequestConfigCallback(requestConfigBuilder -> requestConfigBuilder
//                .setConnectTimeout(5000)
//                .setConnectionRequestTimeout(10_000)
//                .setSocketTimeout(socketTimeout));
//        builder.setHttpClientConfigCallback(httpClientBuilder -> {
//            IOReactorConfig reactorConfig = IOReactorConfig.custom()
//                    .setIoThreadCount(Math.max(concurrentRequest, Runtime.getRuntime().availableProcessors()))
//                    .setSelectInterval(100)
//                    .setSoKeepAlive(true)
//                    .setShutdownGracePeriod(60_000)
//                    .build();
//            httpClientBuilder.setMaxConnTotal(concurrentRequest);
//            httpClientBuilder.setMaxConnPerRoute(concurrentRequest);
//            httpClientBuilder.setDefaultIOReactorConfig(reactorConfig);
//            httpClientBuilder.setKeepAliveStrategy((response, context) -> socketTimeout / 3);
//            return httpClientBuilder;
//        });
//    }

    public <R, T extends KnVO> Map<String, CompletableFuture<List<R>>> knnSearchLibMap(Collection<String> queryStringList,
                                                                                       AiAssistantKn kn,
                                                                                       Function<T, R> mapper,
                                                                                       Function<String, CompletableFuture<Map<String, Object>>> bodyBuilder,
                                                                                       Class<T> type) {
        LinkedHashMap<String, CompletableFuture<List<R>>> map = new LinkedHashMap<>();
        for (String s : queryStringList) {
            if (map.containsKey(s)) {
                continue;
            }
            map.put(s, knnSearchLib(kn, type, bodyBuilder.apply(s))
                    .thenApply(e1 -> e1.stream().map(mapper).collect(Collectors.toList())));
        }
        return map;
    }

    /**
     * 向量搜索知识库
     *
     * @param kn   kn
     * @param type type
     * @param body body
     * @param <T>  知识库
     * @return 知识库
     */
    public <T extends KnVO> CompletableFuture<List<T>> knnSearchLib(AiAssistantKn kn,
                                                                    Class<T> type,
                                                                    CompletableFuture<Map<String, Object>> body) {
        Double minScore = AiUtil.scoreToDouble(kn.getMinScore());
        Double knTop1Score = AiUtil.scoreToDouble(kn.getKnTop1Score());
        String indexName = kn.getKnIndexName();
        return knnSearch(minScore, knTop1Score, indexName, KnnQueryBuilderFuture.completedFuture(type, body));
    }

    /**
     * 获取向量模型
     *
     * @param assistant assistant
     * @return 向量模型
     */
    public EmbeddingModelClient getModel(AiAssistantKn assistant) {
        if (assistant == null) {
            return null;
        }
        String embeddingApiKey = assistant.getEmbeddingApiKey();
        if (!StringUtils.hasText(embeddingApiKey)) {
            return null;
        }
        OpenAiEmbeddingModel[] models = modelMap.computeIfAbsent(assistant.getAssistantId(), e -> {
            OpenAiEmbeddingModel[] arrays = new OpenAiEmbeddingModel[concurrentEmbeddingModelCount];
            for (int i = 0; i < arrays.length; i++) {
                arrays[i] = create(assistant.getEmbeddingApiKey(), assistant.getEmbeddingBaseUrl(),
                        assistant.getEmbeddingModelName(), assistant.getEmbeddingDimensions());
            }
            return arrays;
        });
        return new EmbeddingModelClient(models[modelModIndex++ % models.length],
                assistant.getEmbeddingModelName(),
                assistant.getEmbeddingDimensions(),
                assistant.getEmbeddingMaxRequestSize(),
                aiEmbeddingMapper, executor);
    }

    @Override
    public String toString() {
        return modelMap.keySet().toString();
    }

    /**
     * 创建向量模型
     */
    private OpenAiEmbeddingModel create(String embeddingApiKey,
                                        String embeddingBaseUrl,
                                        String embeddingModelName,
                                        Integer embeddingDimensions) {
        return OpenAiEmbeddingModel.builder()
                .apiKey(embeddingApiKey)
                .baseUrl(embeddingBaseUrl)
                .modelName(embeddingModelName)
                .dimensions(embeddingDimensions)
                .build();
    }

    /**
     * 知识库向量搜索
     * https://www.elastic.co/guide/en/elasticsearch/reference/current/knn-search-api.html
     *
     * @param minScore           minScore
     * @param knTop1Score        knTop1Score
     * @param indexName          indexName
     * @param queryBuilderFuture queryBuilderFuture
     * @param <T>                知识库
     * @return 知识库
     */
    public <T extends KnVO> CompletableFuture<List<T>> knnSearch(
            double minScore,
            Double knTop1Score,
            String indexName,
            KnnQueryBuilderFuture<T> queryBuilderFuture) {
        KnnResponseListenerFuture<T> future = new KnnResponseListenerFuture<>(minScore, knTop1Score, queryBuilderFuture);
        queryBuilderFuture.whenComplete((queryBuilderMap, throwable) -> {
            if (throwable != null) {
                future.completeExceptionally(throwable);
            } else {
                try {
                    // request
                    Request request = new Request("POST", "/" + indexName + "/_search");
                    request.setEntity(EntityBuilder.create()
                            .setContentType(ContentType.APPLICATION_JSON)
                            .setBinary(objectWriter.writeValueAsBytes(queryBuilderMap))
                            .build());
                    future.request = request;
                    future.cancellable = embeddingStore.performRequestAsync(request, future);
                } catch (Throwable e) {
                    future.completeExceptionally(e);
                }
            }
        });
        return future;
    }

    static class KnnResponseListenerFuture<T extends KnVO> extends CompletableFuture<List<T>> implements ResponseListener {
        private final KnnQueryBuilderFuture<T> knnQuery;
        private final double minScore;
        private final Double knTop1Score;
        private Cancellable cancellable;
        private Request request;

        KnnResponseListenerFuture(double minScore,
                                  Double knTop1Score, KnnQueryBuilderFuture<T> knnQuery) {
            this.minScore = minScore;
            this.knTop1Score = knTop1Score;
            this.knnQuery = knnQuery;
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            boolean b = super.cancel(mayInterruptIfRunning);
            if (b && cancellable != null) {
                cancellable.cancel();
            }
            return b;
        }

        @Override
        public void onSuccess(Response response) {
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

            Class<T> type = knnQuery.getType();
            for (Map hit : hitsList) {
                double score = ((Number) hit.get("_score")).doubleValue();
                T source = BeanUtil.toBean((Map<String, Object>) hit.get("_source"), type);
                source.setId(Objects.toString(hit.get("_id"), null));
                source.setScore(score);
                source.setIndexName(Objects.toString(hit.get("_index"), null));
                list.add(source);
            }

            List<T> l = list;
            List<Function<List<T>, List<T>>> afterList = knnQuery.getResponseAfterList();
            if (afterList != null) {
                for (Function<List<T>, List<T>> after : afterList) {
                    l = after.apply(l);
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
        public void onFailure(Exception exception) {
            completeExceptionally(exception);
        }
    }

}
