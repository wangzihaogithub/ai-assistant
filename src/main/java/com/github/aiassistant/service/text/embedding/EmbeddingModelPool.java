package com.github.aiassistant.service.text.embedding;

import com.github.aiassistant.dao.AiEmbeddingMapper;
import com.github.aiassistant.entity.AiAssistantKn;
import com.github.aiassistant.util.StringUtils;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class EmbeddingModelPool {
    /**
     * 向量模型
     */
    private final Map<String, EmbeddingModelClient.Factory[]> modelMap = new ConcurrentHashMap<>();
    /**
     * 每个智能体的向量化模型并发数量. (每个okhttp-client最大64个并发)
     *
     * @see okhttp3.Dispatcher#maxRequests 默认值64
     */
    private final int concurrentEmbeddingModelCount;

    private final AiEmbeddingMapper aiEmbeddingMapper;
    /**
     * 取模轮训下标
     */
    private int modelModIndex = 0;

    public EmbeddingModelPool(AiEmbeddingMapper aiEmbeddingMapper) {
        this(aiEmbeddingMapper, 2);
    }

    public EmbeddingModelPool(AiEmbeddingMapper aiEmbeddingMapper, int concurrentEmbeddingModelCount) {
        this.concurrentEmbeddingModelCount = concurrentEmbeddingModelCount;
        this.aiEmbeddingMapper = aiEmbeddingMapper;
    }

    private static String uniqueKey(Object... keys) {
        return Arrays.toString(keys);
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
        String embeddingBaseUrl = assistant.getEmbeddingBaseUrl();
        String embeddingModelName = assistant.getEmbeddingModelName();
        Integer embeddingDimensions = assistant.getEmbeddingDimensions();
        EmbeddingModelClient.Factory[] models = modelMap.computeIfAbsent(uniqueKey(embeddingModelName, embeddingApiKey, embeddingBaseUrl, embeddingDimensions), e -> {
            EmbeddingModelClient.Factory[] arrays = new EmbeddingModelClient.Factory[concurrentEmbeddingModelCount];
            for (int i = 0; i < arrays.length; i++) {
                arrays[i] = EmbeddingModelClient.builder()
                        .apiKey(embeddingApiKey)
                        .baseUrl(embeddingBaseUrl)
                        .modelName(embeddingModelName)
                        .dimensions(embeddingDimensions)
                        .maxRequestSize(assistant.getEmbeddingMaxRequestSize())
                        .aiEmbeddingMapper(aiEmbeddingMapper)
                        .build();
            }
            return arrays;
        });
        return models[modelModIndex++ % models.length].get();
    }

    public Map<String, EmbeddingModelClient.Factory[]> getModelMap() {
        return modelMap;
    }

//    /**
//     * 创建向量模型
//     */
//    private OpenAiEmbeddingModel create(String embeddingApiKey,
//                                        String embeddingBaseUrl,
//                                        String embeddingModelName,
//                                        Integer embeddingDimensions) {
//        return OpenAiEmbeddingModel.builder()
//                .apiKey(embeddingApiKey)
//                .baseUrl(embeddingBaseUrl)
//                .modelName(embeddingModelName)
//                .dimensions(embeddingDimensions)
//                .build();
//    }
}
