package com.github.aiassistant.service.text.embedding;

import com.github.aiassistant.entity.AiAssistantKn;
import com.github.aiassistant.entity.model.chat.KnVO;
import com.github.aiassistant.platform.JsonUtil;
import com.github.aiassistant.util.AiUtil;
import com.github.aiassistant.util.BeanUtil;
import com.github.aiassistant.util.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.client.entity.EntityBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.message.BasicHeader;
import org.elasticsearch.client.*;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

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

    public KnnApiService(String elasticsearchUrl, String apiKey) {
        this(elasticsearchClient(elasticsearchUrl, apiKey).build());
    }

    public KnnApiService(RestClient embeddingStore) {
        this.embeddingStore = embeddingStore;
    }

    public static RestClientBuilder elasticsearchClient(String url, String apiKey) {
        RestClientBuilder builder = RestClient.builder(HttpHost.create(url));
        if (StringUtils.hasText(apiKey)) {
            builder.setDefaultHeaders(new Header[]{
                    new BasicHeader("Authorization", "ApiKey " + apiKey)
            });
        }
//        settingIO(builder);
        return builder;
    }

    private static <T extends KnVO> List<T> map(Response response,
                                                Class<T> responseType,
                                                List<BiFunction<List<T>, Map, List<T>>> responseAfterList,
                                                double minScore,
                                                Double knTop1Score) throws IOException {
        // response
        Map content = JsonUtil.objectReader().readValue(response.getEntity().getContent(), Map.class);
        Map hits = (Map) content.get("hits");
        List<Map> hitsList = (List<Map>) hits.get("hits");
        List<T> list = new ArrayList<>();

        for (Map hit : hitsList) {
            double score = ((Number) hit.get("_score")).doubleValue();
            T source = BeanUtil.toBean((Map<String, Object>) hit.get("_source"), responseType);
            source.setId(Objects.toString(hit.get("_id"), null));
            source.setScore(score);
            source.setIndexName(Objects.toString(hit.get("_index"), null));
            list.add(source);
        }

        List<T> l = list;
        List<BiFunction<List<T>, Map, List<T>>> afterList = responseAfterList;
        if (afterList != null) {
            for (BiFunction<List<T>, Map, List<T>> after : afterList) {
                l = after.apply(l, content);
            }
        }
        return filter(l, minScore, knTop1Score);
    }

    private static <T extends KnVO> List<T> filter(List<T> list, double minScore, Double knTop1Score) {
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

    /**
     * 批量查询关键词
     *
     * @param queryStringList 查询关键词
     * @param kn              知识库配置
     * @param bodyBuilder     查询条件
     * @param responseType    结果类型
     * @param mapper          如果需要结果转换
     * @param <R>             返回结果类型
     * @param <T>             知识库类型
     * @return 批量查询异步结果
     */
    public <R, T extends KnVO> Map<String, CompletableFuture<List<R>>> knnSearchLibMap(Collection<String> queryStringList,
                                                                                       AiAssistantKn kn,
                                                                                       Function<String, CompletableFuture<Map<String, Object>>> bodyBuilder,
                                                                                       Class<T> responseType,
                                                                                       Function<KnnResponseListenerFuture<T>, CompletableFuture<List<R>>> mapper) {
        LinkedHashMap<String, CompletableFuture<List<R>>> map = new LinkedHashMap<>(queryStringList.size());
        for (String s : queryStringList) {
            if (map.containsKey(s)) {
                continue;
            }
            KnnResponseListenerFuture<T> future = knnSearchLib(kn, responseType, bodyBuilder.apply(s));
            map.put(s, mapper != null ? mapper.apply(future) : (CompletableFuture) future);
        }
        return map;
    }

    /**
     * 批量查询关键词
     *
     * @param queryStringList 查询关键词
     * @param kn              知识库配置
     * @param bodyBuilder     查询条件
     * @param responseType    结果类型
     * @param <T>             知识库类型
     * @param consumer        如果需要结果处理
     * @return 批量查询异步结果
     */
    public <T extends KnVO> Map<String, KnnResponseListenerFuture<T>> knnSearchLibMap(Collection<String> queryStringList,
                                                                                      AiAssistantKn kn,
                                                                                      Function<String, CompletableFuture<Map<String, Object>>> bodyBuilder,
                                                                                      Class<T> responseType,
                                                                                      Consumer<KnnResponseListenerFuture<T>> consumer) {
        LinkedHashMap<String, KnnResponseListenerFuture<T>> map = new LinkedHashMap<>(queryStringList.size());
        for (String s : queryStringList) {
            if (map.containsKey(s)) {
                continue;
            }
            KnnResponseListenerFuture<T> future = knnSearchLib(kn, responseType, bodyBuilder.apply(s));
            if (consumer != null) {
                consumer.accept(future);
            }
            map.put(s, future);
        }
        return map;
    }

    /**
     * 批量查询关键词
     *
     * @param queryStringList 查询关键词
     * @param kn              知识库配置
     * @param bodyBuilder     查询条件
     * @param responseType    结果类型
     * @param <T>             知识库类型
     * @return 批量查询异步结果
     */
    public <T extends KnVO> Map<String, KnnResponseListenerFuture<T>> knnSearchLibMap(Collection<String> queryStringList,
                                                                                      AiAssistantKn kn,
                                                                                      Function<String, CompletableFuture<Map<String, Object>>> bodyBuilder,
                                                                                      Class<T> responseType) {
        LinkedHashMap<String, KnnResponseListenerFuture<T>> map = new LinkedHashMap<>(queryStringList.size());
        for (String s : queryStringList) {
            if (map.containsKey(s)) {
                continue;
            }
            map.put(s, knnSearchLib(kn, responseType, bodyBuilder.apply(s)));
        }
        return map;
    }

    /**
     * 向量搜索知识库
     *
     * @param kn           kn
     * @param responseType responseType
     * @param requestBody  body
     * @param <T>          知识库
     * @return 知识库
     */
    public <T extends KnVO> KnnResponseListenerFuture<T> knnSearchLib(AiAssistantKn kn,
                                                                      Class<T> responseType,
                                                                      CompletableFuture<Map<String, Object>> requestBody) {
        Double minScore = AiUtil.scoreToDouble(kn.getMinScore());
        Double knTop1Score = AiUtil.scoreToDouble(kn.getKnTop1Score());
        String indexName = kn.getKnIndexName();
        return knnSearch(minScore, knTop1Score, indexName, KnnQueryBuilderFuture.completedFuture(responseType, requestBody));
    }

    /**
     * 知识库向量搜索
     * <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/knn-search-api.html">https://www.elastic.co/guide/en/elasticsearch/reference/current/knn-search-api.html</a>
     *
     * @param minScore           minScore
     * @param knTop1Score        knTop1Score
     * @param indexName          indexName
     * @param queryBuilderFuture queryBuilderFuture
     * @param <T>                知识库
     * @return 知识库
     */
    public <T extends KnVO> KnnResponseListenerFuture<T> knnSearch(
            double minScore,
            Double knTop1Score,
            String indexName,
            KnnQueryBuilderFuture<T> queryBuilderFuture) {
        Class<T> responseType = queryBuilderFuture.getResponseType();
        List<BiFunction<List<T>, Map, List<T>>> responseAfterList = queryBuilderFuture.getResponseAfterList();
        KnnResponseListenerFuture<T> future = new KnnResponseListenerFuture<>(queryBuilderFuture, indexName);
        queryBuilderFuture.whenComplete((queryBuilderMap, throwable) -> {
            if (throwable != null) {
                future.completeExceptionally(throwable);
            } else {
                try {
                    byte[] requestBody = objectWriter.writeValueAsBytes(queryBuilderMap);
                    // request
                    Request request = new Request("POST", "/" + indexName + "/_search");
                    request.setEntity(EntityBuilder.create()
                            .setContentType(ContentType.APPLICATION_JSON)
                            .setBinary(requestBody)
                            .build());
                    Cancellable cancellable = embeddingStore.performRequestAsync(request, new ResponseListener() {
                        @Override
                        public void onSuccess(Response response) {
                            try {
                                List<T> list = map(response, responseType, responseAfterList, minScore, knTop1Score);
                                future.complete(list);
                            } catch (Exception e) {
                                future.completeExceptionally(e);
                            }

                        }

                        @Override
                        public void onFailure(Exception exception) {
                            future.completeExceptionally(exception);
                        }
                    });
                    future.setRequest(new KnnResponseListenerFuture.Request() {
                        @Override
                        public byte[] getRequestBodyBytes() {
                            return requestBody;
                        }

                        @Override
                        public void cancel() {
                            cancellable.cancel();
                        }
                    });
                } catch (Throwable e) {
                    future.completeExceptionally(e);
                }
            }
        });
        return future;
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

    @Override
    public String toString() {
        return String.valueOf(embeddingStore);
    }
}
