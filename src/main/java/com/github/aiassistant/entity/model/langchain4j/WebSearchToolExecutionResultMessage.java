package com.github.aiassistant.entity.model.langchain4j;

import com.github.aiassistant.entity.model.chat.WebSearchResultVO;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.ToolExecutionResultMessage;

import java.util.List;

public class WebSearchToolExecutionResultMessage extends ToolExecutionResultMessage {
    private final List<WebSearchResultVO> resultList;

    public WebSearchToolExecutionResultMessage(ToolExecutionRequest request, String text, List<WebSearchResultVO> resultList) {
        super(request.id(), request.name(), text);
        this.resultList = resultList;
    }

    public List<WebSearchResultVO> getResultList() {
        return resultList;
    }
}
