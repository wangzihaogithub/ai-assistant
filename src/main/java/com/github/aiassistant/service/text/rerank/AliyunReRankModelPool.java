package com.github.aiassistant.service.text.rerank;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 使用阿里云Rerank模型池
 */
public class AliyunReRankModelPool {
    private final Map<String, AliyunReRankModel> modelMap = new ConcurrentHashMap<>(3);

    public AliyunReRankModelPool() {
    }

    public AliyunReRankModel getModel(String apiKey) {
        return getModel(apiKey, AliyunReRankModel.DEFAULT_MODEL);
    }

    public AliyunReRankModel getModel(String apiKey, String modelName) {
        return modelMap.computeIfAbsent(String.join("-", apiKey, modelName), e -> {
            AliyunReRankModel model = new AliyunReRankModel(apiKey);
            model.setModel(modelName);
            return model;
        });
    }

    public Map<String, AliyunReRankModel> getModelMap() {
        return modelMap;
    }
}
