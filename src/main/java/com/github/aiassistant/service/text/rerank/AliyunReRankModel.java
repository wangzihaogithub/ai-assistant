package com.github.aiassistant.service.text.rerank;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.github.aiassistant.platform.JsonUtil;
import com.github.aiassistant.util.FutureUtil;
import com.github.aiassistant.util.Lists;
import okhttp3.*;
import okio.BufferedSink;

import java.io.IOException;
import java.io.OutputStream;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collectors;

/**
 * 使用阿里云Rerank模型进行rerank
 * <p>
 * -------------------------------------------------------------------------------------
 * ｜ 模型中文名  ｜ 模型英文名     ｜ 最大Document数量 ｜ 单行最大输入Token ｜ 最大输入Token ｜
 * -------------------------------------------------------------------------------------
 * ｜ 通用文本排序 ｜ gte-rerank   ｜500              ｜  4,000          ｜ 30,000      ｜
 * -------------------------------------------------------------------------------------
 * <p>
 * 接口文档 https://help.aliyun.com/zh/model-studio/text-rerank-api
 */
public class AliyunReRankModel implements ReRankModel {
    public static final String DEFAULT_MODEL = "gte-rerank-v2";
    private static final JsonUtil.ObjectWriter objectWriter = JsonUtil.objectWriter();
    private static final JsonUtil.ObjectReader objectReader = JsonUtil.objectReader();
    private static final MediaType mediaType = MediaType.parse("application/json");
    private Duration callTimeout = Duration.ofSeconds(60);
    private Duration connectTimeout = Duration.ofSeconds(3);
    private int maxRequestDocuments = 500;
    private volatile OkHttpClient client;
    private String apiKey;
    private String url = "https://dashscope.aliyuncs.com/api/v1/services/rerank/text-rerank/text-rerank";
    /**
     * 指明需要调用的模型，仅可选择gte-rerank-v2
     */
    private String model = DEFAULT_MODEL;
    private boolean destroy;

    public AliyunReRankModel() {
    }

    public AliyunReRankModel(String apiKey) {
        this.apiKey = apiKey;
    }

    @Override
    public String toString() {
        return model;
    }

    @Override
    public void destroy() {
        destroy = true;
        OkHttpClient client = this.client;
        this.client = null;
        if (client != null) {
            try {
                client.connectionPool().evictAll();
            } catch (Exception e) {
                // ignore
            }
            Cache cache = client.cache();
            if (cache != null) {
                try {
                    cache.close();
                } catch (Exception e) {
                    // ignore
                }
            }
        }
    }

    public int getMaxRequestDocuments() {
        return maxRequestDocuments;
    }

    public void setMaxRequestDocuments(int maxRequestDocuments) {
        this.maxRequestDocuments = maxRequestDocuments;
    }

    @Override
    public <E, M extends ReRankModel, K extends SortKey<E>> CompletableFuture<List<K>> sortKey(String query, Collection<E> documents, Function<E, String> reRankKey, int topN, BiFunction<M, List<K>, CompletableFuture<List<K>>> filter) {
        OkHttpClient client = getClient();
        LinkedHashMap<String, List<E>> groupByKeyMap = groupByKey(documents, reRankKey);
        ArrayList<String> documentsCopy = new ArrayList<>(groupByKeyMap.keySet());
        if (documentsCopy.size() < maxRequestDocuments) {
            return request(query, client, groupByKeyMap, documentsCopy, topN, filter);
        } else {
            List<List<String>> partitionList = Lists.partition(documentsCopy, maxRequestDocuments);
            List<CompletableFuture<List<K>>> futures = new ArrayList<>();
            for (List<String> partition : partitionList) {
                CompletableFuture<List<K>> request = request(query, client, groupByKeyMap, partition, topN, filter);
                futures.add(request);
            }
            return FutureUtil.allOf(futures)
                    .thenApply(lists -> lists.stream()
                            .flatMap(Collection::stream)
                            .sorted(Comparator.comparingDouble((ToDoubleFunction<K>) SortKey::getSimilarity).reversed())
                            .limit(topN)
                            .collect(Collectors.toList()));
        }
    }

    private <E, M extends ReRankModel, K extends SortKey<E>> CompletableFuture<List<K>> request(
            String query, OkHttpClient client,
            LinkedHashMap<String, List<E>> groupByKeyMap,
            List<String> documents, int topN,
            BiFunction<M, List<K>, CompletableFuture<List<K>>> filter) {
        Request request = new Request.Builder()
                .url(url)
                .post(new RequestBody() {
                    @Override
                    public MediaType contentType() {
                        return mediaType;
                    }

                    @Override
                    public void writeTo(BufferedSink sink) throws IOException {
                        RerankRequest rerankRequest = new RerankRequest();
                        rerankRequest.setModel(model);
                        RerankRequest.Input input = new RerankRequest.Input();
                        input.setQuery(query);
                        input.setDocuments(documents);
                        rerankRequest.setInput(input);
                        RerankRequest.Parameters parameters = new RerankRequest.Parameters();
                        parameters.setTopN(topN);
                        parameters.setReturnDocuments(false);
                        rerankRequest.setParameters(parameters);
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
                        }, rerankRequest);
                    }
                })
                .build();
        // 异步调用
        Call call = client.newCall(request);
        TopNCompletableFuture<List<K>> future = new TopNCompletableFuture<>(call);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                future.completeExceptionally(e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    ResponseBody body = response.body();
                    if (response.isSuccessful() && body != null) {
                        RerankResponse rerankResponse = objectReader.readValue(body.byteStream(), RerankResponse.class);
                        List<K> list = new ArrayList<>(groupByKeyMap.size());
                        for (RerankResponse.Result result : rerankResponse.output.results) {
                            String key = documents.get(result.index);
                            List<E> valueList = groupByKeyMap.get(key);
                            list.add((K) new SortKeyImpl<>(valueList, key, result.relevanceScore));
                        }
                        if (filter == null) {
                            future.complete(list);
                        } else {
                            CompletableFuture<List<K>> f = filter.apply((M) AliyunReRankModel.this, list);
                            FutureUtil.join(f, future);
                        }
                    } else {
                        String string = body == null ? "" : body.string();
                        future.completeExceptionally(new IOException("rerank fail! code=" + response.code() + ", body=" + string + ", headers=" + response.headers()));
                    }
                } catch (Exception e) {
                    future.completeExceptionally(e);
                }
            }
        });
        return future;
    }

    public Duration getCallTimeout() {
        return callTimeout;
    }

    public void setCallTimeout(Duration callTimeout) {
        this.callTimeout = callTimeout;
    }

    public Duration getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(Duration connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    private OkHttpClient getClient() {
        if (destroy) {
            throw new IllegalStateException("AliyunReRankModel(" + model + ") is destroy!");
        }
        if (client == null) {
            synchronized (this) {
                if (client == null) {
                    OkHttpClient.Builder okHttpClientBuilder = new OkHttpClient.Builder()
                            .callTimeout(callTimeout)
                            .connectTimeout(connectTimeout)
                            .readTimeout(callTimeout)
                            .writeTimeout(callTimeout);

                    Map<String, String> headers = new HashMap<>();
                    headers.put("Authorization", "Bearer " + apiKey);
                    okHttpClientBuilder.addInterceptor(chain -> {
                        Request.Builder builder = chain.request().newBuilder();
                        headers.forEach(builder::addHeader);
                        return chain.proceed(builder.build());
                    });
                    okHttpClientBuilder.dispatcher(new Dispatcher(new ThreadPoolExecutor(0, Integer.MAX_VALUE, 60, TimeUnit.SECONDS,
                            new SynchronousQueue<>(), runnable -> {
                        Thread result = new Thread(runnable);
                        result.setName("OkHttp ai-rerank-" + result.getId());
                        result.setDaemon(false);
                        return result;
                    })));
                    client = okHttpClientBuilder.build();
                }
            }
        }
        return client;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public static class TopNCompletableFuture<T> extends CompletableFuture<T> {
        private final Call call;
        private final long startTimeMillis = System.currentTimeMillis();
        private long endTimeMillis;

        public TopNCompletableFuture(Call call) {
            this.call = call;
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            if (isCancelled()) {
                return false;
            }
            call.cancel();
            return super.cancel(mayInterruptIfRunning);
        }

        @Override
        public boolean complete(T value) {
            endTimeMillis = System.currentTimeMillis();
            return super.complete(value);
        }

        @Override
        public boolean completeExceptionally(Throwable ex) {
            endTimeMillis = System.currentTimeMillis();
            return super.completeExceptionally(ex);
        }

        public long getStartTimeMillis() {
            return startTimeMillis;
        }

        public long getEndTimeMillis() {
            return endTimeMillis;
        }

        @Override
        public String toString() {
            return "(" + (endTimeMillis - startTimeMillis) + "ms)" + super.toString();
        }
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class RerankResponse {
        private Output output;
        private String requestId;
        private Usage usage;

        public Usage getUsage() {
            return usage;
        }

        public void setUsage(Usage usage) {
            this.usage = usage;
        }

        public String getRequestId() {
            return requestId;
        }

        public void setRequestId(String requestId) {
            this.requestId = requestId;
        }

        public Output getOutput() {
            return output;
        }

        public void setOutput(Output output) {
            this.output = output;
        }

        @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
        public static class Usage {
            private Integer totalTokens;

            public Integer getTotalTokens() {
                return totalTokens;
            }

            public void setTotalTokens(Integer totalTokens) {
                this.totalTokens = totalTokens;
            }
        }

        @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
        public static class Output {
            private List<Result> results;

            public List<Result> getResults() {
                return results;
            }

            public void setResults(List<Result> results) {
                this.results = results;
            }
        }

        @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
        public static class Result {
            private Document document;
            private Integer index;
            private Double relevanceScore;

            public Document getDocument() {
                return document;
            }

            public void setDocument(Document document) {
                this.document = document;
            }

            public Integer getIndex() {
                return index;
            }

            public void setIndex(Integer index) {
                this.index = index;
            }

            public Double getRelevanceScore() {
                return relevanceScore;
            }

            public void setRelevanceScore(Double relevanceScore) {
                this.relevanceScore = relevanceScore;
            }
        }

        @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
        public static class Document {
            private String text;

            public String getText() {
                return text;
            }

            public void setText(String text) {
                this.text = text;
            }
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class RerankRequest {
        /**
         * 指明需要调用的模型，仅可选择gte-rerank-v2
         */
        private String model;
        private Input input;
        private Parameters parameters;

        public Input getInput() {
            return input;
        }

        public void setInput(Input input) {
            this.input = input;
        }

        public Parameters getParameters() {
            return parameters;
        }

        public void setParameters(Parameters parameters) {
            this.parameters = parameters;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
        public static class Input {
            /**
             * query最大长度不能超过4,000个Token
             * "什么是文本排序模型"
             */
            private String query;
            /**
             * 待排序的候选document列表
             * [
             * "文本排序模型广泛用于搜索引擎和推荐系统中，它们根据文本相关性对候选文本进行排序",
             * "量子计算是计算科学的一个前沿领域",
             * "预训练语言模型的发展给文本排序模型带来了新的进展"
             * ]
             */
            private List<String> documents;

            public String getQuery() {
                return query;
            }

            public void setQuery(String query) {
                this.query = query;
            }

            public List<String> getDocuments() {
                return documents;
            }

            public void setDocuments(List<String> documents) {
                this.documents = documents;
            }

        }

        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
        public static class Parameters {
            /**
             * 排序返回的top文档数量，未指定时默认返回全部候选文档，如果指定的top_n值大于输入的候选document数量，返回全部候选文档
             */
            private Integer topN;
            /**
             * 返回的排序结果列表中是否返回每一条document原文，默认值False
             */
            private Boolean returnDocuments;

            public Integer getTopN() {
                return topN;
            }

            public void setTopN(Integer topN) {
                this.topN = topN;
            }

            public Boolean getReturnDocuments() {
                return returnDocuments;
            }

            public void setReturnDocuments(Boolean returnDocuments) {
                this.returnDocuments = returnDocuments;
            }
        }

    }
}
