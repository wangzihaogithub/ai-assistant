package com.github.aiassistant.service.text.nlu;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.github.aiassistant.platform.JsonUtil;
import com.github.aiassistant.util.StringUtils;
import okhttp3.*;
import okio.BufferedSink;

import java.io.IOException;
import java.io.OutputStream;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * 使用阿里云Open nlu模型
 * 虽然OpenNLU已经在大量任务上训练且具备泛化性，但由于实际NLU任务的多样性、复杂性，其在不同具体任务上的效果可能有较大差别，请谨慎评估模型效果是否符合需求。
 * <p>
 * 接口文档 https://help.aliyun.com/zh/model-studio/opennlu-api
 */
public class AliyunOpenNluModel implements NluModel {
    private static final JsonUtil.ObjectWriter objectWriter = JsonUtil.objectWriter();
    private static final JsonUtil.ObjectReader objectReader = JsonUtil.objectReader();
    private static final MediaType mediaType = MediaType.parse("application/json");
    private Duration callTimeout = Duration.ofSeconds(60);
    private Duration connectTimeout = Duration.ofSeconds(3);
    private volatile OkHttpClient client;
    /**
     * 主账号的apiKey
     */
    private String apiKey;
    private String url = "https://dashscope.aliyuncs.com/api/v1/services/nlp/nlu/understanding";
    /**
     * 指明需要调用的模型，opennlu-v1(开箱即用的文本理解大模型，适用于中文、英文零样本条件下进行文本理解任务，如信息抽取、文本分类等。)
     */
    private String model = "opennlu-v1";

    public AliyunOpenNluModel() {
    }

    public AliyunOpenNluModel(String apiKey) {
        this.apiKey = apiKey;
    }

    @Override
    public CompletableFuture<List<String>> classification(String document, Collection<String> labels) {
        return request(document, labels, "classification");
    }

    @Override
    public CompletableFuture<List<String>> extraction(String document, Collection<String> labels) {
        return request(document, labels, "extraction");
    }

    public CompletableFuture<List<String>> request(String document, Collection<String> labels, String task) {
        OkHttpClient client = getClient();
        Request request = new Request.Builder()
                .url(url)
                .post(new RequestBody() {
                    @Override
                    public MediaType contentType() {
                        return mediaType;
                    }

                    @Override
                    public void writeTo(BufferedSink sink) throws IOException {
                        OpenNluRequest openNluRequest = new OpenNluRequest();
                        openNluRequest.setModel(model);

                        OpenNluRequest.Input input = new OpenNluRequest.Input();
                        input.setTask(task);
                        input.setSentence(document);
                        input.setLabels(String.join("，", labels));
                        openNluRequest.setInput(input);

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
                        }, openNluRequest);
                    }
                })
                .build();
        // 异步调用
        Call call = client.newCall(request);
        TopNCompletableFuture<List<String>> future = new TopNCompletableFuture<>(call);
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
                        OpenNluResponse nluResponse = objectReader.readValue(body.byteStream(), OpenNluResponse.class);
                        if (StringUtils.hasText(nluResponse.getCode())) {
                            future.completeExceptionally(new IOException(
                                    String.format("classification fail! request_id=%s,code=%s, message=%s",
                                            nluResponse.getRequestId(), nluResponse.getCode(), nluResponse.getMessage())));
                        } else {
                            String[] strings = Optional.ofNullable(nluResponse.getOutput())
                                    .map(OpenNluResponse.Output::getText)
                                    .map(e -> e.endsWith(";") ? e.substring(0, e.length() - 1) : e)
                                    .map(e -> e.split(","))
                                    .orElse(new String[0]);
                            future.complete(Arrays.asList(strings));
                        }
                    } else {
                        String string = body == null ? "" : body.string();
                        future.completeExceptionally(new IOException("classification http fail! httpCode=" + response.code() + ", body=" + string));
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
    public static class OpenNluResponse {
        private Output output;
        /**
         * 200（HTTPStatus.OK）表示请求成功，否则表示请求失败，可以通过code获取错误码，通过message字段获取错误详细信息。
         */
        private Integer statusCode;
        private String requestId;
        /**
         * 表示请求失败，表示错误码，成功忽略。
         */
        private String code;
        /**
         * 失败，表示失败详细信息，成功忽略。
         */
        private String message;
        private Usage usage;

        public Integer getStatusCode() {
            return statusCode;
        }

        public void setStatusCode(Integer statusCode) {
            this.statusCode = statusCode;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

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
            private Integer outputTokens;
            private Integer inputTokens;

            public Integer getTotalTokens() {
                return totalTokens;
            }

            public void setTotalTokens(Integer totalTokens) {
                this.totalTokens = totalTokens;
            }

            public Integer getOutputTokens() {
                return outputTokens;
            }

            public void setOutputTokens(Integer outputTokens) {
                this.outputTokens = outputTokens;
            }

            public Integer getInputTokens() {
                return inputTokens;
            }

            public void setInputTokens(Integer inputTokens) {
                this.inputTokens = inputTokens;
            }
        }

        @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
        public static class Output {
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
    public static class OpenNluRequest {
        /**
         * 指明需要调用的模型,opennlu-v1opennlu-v1(开箱即用的文本理解大模型，适用于中文、英文零样本条件下进行文本理解任务，如信息抽取、文本分类等。)
         */
        private String model;
        private Input input;

        public Input getInput() {
            return input;
        }

        public void setInput(Input input) {
            this.input = input;
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
             * 任务类型，可选项是 extraction classification，默认为extraction
             */
            private String task;
            /**
             * 用户输入的需要处理的文本内容，支持中英文。（input最长限制1024个tokens，为input所有字段的总和）
             * 老师今天表扬我了
             */
            private String sentence;
            /**
             * 对于抽取任务，label为需要抽取的类型名称。对于分类任务，label为分类体系。不同标签用中文逗号分隔。
             * 抽取任务：
             * 实体识别：人名，地名
             * 阅读理解：小明的年龄是多少？，小明的身高是多少？
             * 分类任务：
             * 主题分类：体育新闻，娱乐新闻
             * 情感分类：积极，消极
             */
            private String labels;


            public String getTask() {
                return task;
            }

            public void setTask(String task) {
                this.task = task;
            }

            public String getSentence() {
                return sentence;
            }

            public void setSentence(String sentence) {
                this.sentence = sentence;
            }

            public String getLabels() {
                return labels;
            }

            public void setLabels(String labels) {
                this.labels = labels;
            }
        }
    }
}
