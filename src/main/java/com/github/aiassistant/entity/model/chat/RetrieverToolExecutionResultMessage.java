package com.github.aiassistant.entity.model.chat;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.ToolExecutionResultMessage;

import java.util.List;
import java.util.Map;

/**
 * 召回工具结果
 */
public class RetrieverToolExecutionResultMessage<T extends QaKnVO> extends ToolExecutionResultMessage {
    private final Map<String, List<T>> resultMap;

    public RetrieverToolExecutionResultMessage(ToolExecutionRequest request, String text, Map<String, List<T>> resultMap) {
        super(request.id(), request.name(), text);
        this.resultMap = resultMap;
    }

    public Map<String, List<T>> getResultMap() {
        return resultMap;
    }
}
