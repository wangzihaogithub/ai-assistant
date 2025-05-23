package com.github.aiassistant.exception;

import com.github.aiassistant.service.text.tools.ResultToolExecutor;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.output.Response;

import java.util.List;

/**
 * AI工具执行过程中出现错误
 */
public class ToolExecuteException extends AiAssistantException {
    private final List<ResultToolExecutor> toolExecutorList;
    private final Response<AiMessage> toolExecutionRequests;

    public ToolExecuteException(String message, Throwable cause,
                                List<ResultToolExecutor> toolExecutorList,
                                Response<AiMessage> toolExecutionRequests) {
        super(message, cause);
        this.toolExecutorList = toolExecutorList;
        this.toolExecutionRequests = toolExecutionRequests;
    }

    public List<ResultToolExecutor> getToolExecutorList() {
        return toolExecutorList;
    }

    public Response<AiMessage> getToolExecutionRequests() {
        return toolExecutionRequests;
    }
}
