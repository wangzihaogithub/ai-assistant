package com.github.aiassistant;

import com.github.aiassistant.dao.AiJsonschemaMapper;
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
import com.github.aiassistant.util.ThrowableUtil;
import com.mysql.cj.jdbc.MysqlDataSource;
import dev.langchain4j.model.openai.OpenAiChatClient;

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

    public static OpenAiChatClient openAiChatClient(String baseUrl, String apiKey, String modelName) {
        return OpenAiChatClient.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .baseUrl(baseUrl)
                .build();
    }

    public static EmbeddingModelClient.Factory openAiEmbeddingClient(String baseUrl, String apiKey, String modelName, int dimensions) {
        return EmbeddingModelClient.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .baseUrl(baseUrl)
                .dimensions(dimensions)
                .build();
    }

    public static EmbeddingModelClient.Factory aliyunEmbeddingV4(String apiKey, int dimensions) {
        return EmbeddingModelClient.builder()
                .apiKey(apiKey)
                .modelName("text-embedding-v4")
                .baseUrl("https://dashscope.aliyuncs.com/compatible-mode/v1")
                .dimensions(dimensions)
                .build();
    }

    public static EmbeddingModelClient.Factory aliyunEmbeddingV3(String apiKey, int dimensions) {
        return EmbeddingModelClient.builder()
                .apiKey(apiKey)
                .modelName("text-embedding-v3")
                .baseUrl("https://dashscope.aliyuncs.com/compatible-mode/v1")
                .dimensions(dimensions)
                .build();
    }

    public static EmbeddingReRankModel aliyunEmbeddingV3ReRank(String apiKey, int dimensions) {
        return new EmbeddingReRankModel(aliyunEmbeddingV3(apiKey, dimensions).get());
    }

    public static EmbeddingReRankModel aliyunEmbeddingV3ReRank1024(String apiKey) {
        return new EmbeddingReRankModel(aliyunEmbeddingV3(apiKey, 1024).get());
    }

    public static AliyunReRankModel aliyunGteRerankV2(String apiKey) {
        return new AliyunReRankModel(apiKey);
    }

    public static AliyunOpenNluModel aliyunNlu(String apiKey) {
        return new AliyunOpenNluModel(apiKey);
    }

    public static AiToolServiceImpl aiToolService(Mybatis3DAOProvider daoProvider, Function<String, Tools> toolsMap) {
        AiToolMapper aiToolMapper = daoProvider.getMapper(AiToolMapper.class);
        AiToolParameterMapper aiToolParameterMapper = daoProvider.getMapper(AiToolParameterMapper.class);
        return new AiToolServiceImpl(aiToolMapper, aiToolParameterMapper, toolsMap);
    }

    public static Mybatis3DAOProvider daoProvider(DataSource dataSource) {
        return new Mybatis3DAOProvider(dataSource);
    }

    public static LlmJsonSchemaApiService jsonSchemaApiService(DataSource dataSource, Function<String, Tools> toolsMap) {
        return jsonSchemaApiService(daoProvider(dataSource), toolsMap);
    }

    public static LlmJsonSchemaApiService jsonSchemaApiService(Mybatis3DAOProvider daoProvider, Function<String, Tools> toolsMap) {
        return new LlmJsonSchemaApiService(daoProvider.getMapper(AiJsonschemaMapper.class), aiToolService(daoProvider, toolsMap));
    }

    public static AiApplication aiApplication(DAOProvider daoProvider,
                                              KnnApiService knnApiService,
                                              Function<String, Tools> toolsMap) {
        return new AiApplication(null, daoProvider, knnApiService, toolsMap, null);
    }

    public static AiApplication aiApplication(DataSource dataSource,
                                              KnnApiService knnApiService,
                                              Function<String, Tools> toolsMap) {
        return new AiApplication(null, daoProvider(dataSource), knnApiService, toolsMap, null);
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
