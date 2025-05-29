package com.github.aiassistant;

import com.github.aiassistant.dao.AiToolMapper;
import com.github.aiassistant.dao.AiToolParameterMapper;
import com.github.aiassistant.platform.AliyunAppsClient;
import com.github.aiassistant.platform.Mybatis3DAOProvider;
import com.github.aiassistant.service.jsonschema.LlmJsonSchemaApiService;
import com.github.aiassistant.service.text.embedding.EmbeddingModelClient;
import com.github.aiassistant.service.text.embedding.KnnApiService;
import com.github.aiassistant.service.text.nlu.AliyunOpenNluModel;
import com.github.aiassistant.service.text.rerank.AliyunReRankModel;
import com.github.aiassistant.service.text.rerank.EmbeddingReRankModel;
import com.github.aiassistant.service.text.tools.AiToolServiceImpl;
import com.github.aiassistant.service.text.tools.Tools;
import com.github.aiassistant.service.text.tools.WebSearchService;
import com.github.aiassistant.service.text.tools.functioncall.UrlReadTools;
import com.github.aiassistant.util.StringUtils;
import com.github.aiassistant.util.ThrowableUtil;
import com.mysql.cj.jdbc.MysqlDataSource;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.message.BasicHeader;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;

import javax.sql.DataSource;
import java.util.function.Function;

public class AiBuilders {

//    private static AiAssistantKn assistant() {
//        AiAssistantKn assistant = new AiAssistantKn();
//        assistant.setAssistantId("cnwyjob");
//        assistant.setEmbeddingApiKey("sk-xxx");
//        assistant.setEmbeddingDimensions(1024);
//        assistant.setEmbeddingBaseUrl("https://dashscope.aliyuncs.com/compatible-mode/v1");
//        assistant.setEmbeddingModelName("text-embedding-v3");
//
//        assistant.setKnIndexName("cnwy_job_prod_index_v2");
//        assistant.setKnLimit(5);
//        assistant.setMinScore(0L);
//        assistant.setVectorFieldName("jobVector");
//        return assistant;
//    }

    public static EmbeddingModelClient aliyunEmbeddingV3(String apiKey, int dimensions) {
        String modelName = "text-embedding-v3";
        return new EmbeddingModelClient(OpenAiEmbeddingModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .baseUrl("https://dashscope.aliyuncs.com/compatible-mode/v1")
                .dimensions(dimensions)
                .build(),
                modelName, dimensions, null, null, null);
    }

    public static EmbeddingReRankModel aliyunEmbeddingV3ReRank(String apiKey, int dimensions) {
        return new EmbeddingReRankModel(aliyunEmbeddingV3(apiKey, dimensions));
    }

    public static EmbeddingReRankModel aliyunEmbeddingV3ReRank1024(String apiKey) {
        return new EmbeddingReRankModel(aliyunEmbeddingV3(apiKey, 1024));
    }

    public static AliyunReRankModel aliyunGteRerankV2(String apiKey) {
        return new AliyunReRankModel(apiKey);
    }

    public static AliyunOpenNluModel aliyunNlu(String apiKey) {
        return new AliyunOpenNluModel(apiKey);
    }

    public static AiToolServiceImpl aiToolService(DataSource dataSource, Function<String, Tools> toolsMap) {
        Mybatis3DAOProvider provider = new Mybatis3DAOProvider(dataSource);
        AiToolMapper aiToolMapper = provider.getMapper(AiToolMapper.class);
        AiToolParameterMapper aiToolParameterMapper = provider.getMapper(AiToolParameterMapper.class);
        return new AiToolServiceImpl(aiToolMapper, aiToolParameterMapper, toolsMap);
    }

    public static LlmJsonSchemaApiService llmJsonSchemaApiService(DataSource dataSource, Function<String, Tools> toolsMap) {
        return new LlmJsonSchemaApiService(aiToolService(dataSource, toolsMap));
    }

    public static LlmJsonSchemaApiService llmJsonSchemaApiService(AiToolServiceImpl aiToolService) {
        return new LlmJsonSchemaApiService(aiToolService);
    }

    public static KnnApiService knnApiService(String elasticsearchUrl, String apiKey) {
        return new KnnApiService(elasticsearchClient(elasticsearchUrl, apiKey).build());
    }

    public static RestClientBuilder elasticsearchClient(String url, String apiKey) {
        RestClientBuilder builder = RestClient.builder(HttpHost.create(url));
        if (StringUtils.hasText(apiKey)) {
            builder.setDefaultHeaders(new Header[]{
                    new BasicHeader("Authorization", "ApiKey " + apiKey)
            });
        }
        return builder;
    }

    public static AiApplication aiApplication(DataSource dataSource,
                                              RestClient embeddingStore,
                                              Function<String, Tools> toolsMap) {
        return new AiApplication(dataSource, embeddingStore, toolsMap, null);
    }

    public static WebSearchService webSearchService() {
        return new WebSearchService();
    }

    public static UrlReadTools urlReadTools() {
        return new UrlReadTools("AiBuilders", 1000, 5000, 3);
    }

    public static AliyunAppsClient aliyunAppsClient(String apiKey, String appId) {
        return new AliyunAppsClient(apiKey, appId);
    }

    public static DataSource mysqlDataSource(String url, String username, String password) {
        MysqlDataSource dataSource = new MysqlDataSource();
        dataSource.setUrl(url);
        dataSource.setUser(username);
        dataSource.setPassword(password);
        try {
            dataSource.setZeroDateTimeBehavior("CONVERT_TO_NULL");
        } catch (Exception e) {
            ThrowableUtil.sneakyThrows(e);
        }
        return dataSource;
    }

}
