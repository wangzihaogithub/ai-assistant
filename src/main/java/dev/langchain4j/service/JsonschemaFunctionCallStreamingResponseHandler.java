package dev.langchain4j.service;

import com.github.aiassistant.service.jsonschema.LlmJsonSchemaApiService;
import com.github.aiassistant.service.text.ChatStreamingResponseHandler;
import com.github.aiassistant.service.text.FunctionCallStreamingResponseHandler;
import com.github.aiassistant.service.text.sseemitter.SseHttpResponse;
import com.github.aiassistant.service.text.tools.Tools;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.output.Response;

import java.util.List;
import java.util.concurrent.Executor;

/**
 * Jsonschema工具调用
 */
class JsonschemaFunctionCallStreamingResponseHandler extends FunctionCallStreamingResponseHandler {

    JsonschemaFunctionCallStreamingResponseHandler(String modelName,
                                                   StreamingChatLanguageModel chatModel, ChatMemory chatMemory,
                                                   ChatStreamingResponseHandler handler,
                                                   LlmJsonSchemaApiService llmJsonSchemaApiService,
                                                   List<Tools.ToolMethod> toolMethodList,
                                                   boolean isSupportChineseToolName,
                                                   int baseMessageIndex, int addMessageCount, Long readTimeoutMs, Executor executor) {
        super(modelName, chatModel, chatMemory, handler, llmJsonSchemaApiService,
                toolMethodList, isSupportChineseToolName, baseMessageIndex, addMessageCount, readTimeoutMs, executor);
    }

    JsonschemaFunctionCallStreamingResponseHandler(FunctionCallStreamingResponseHandler parent) {
        super(parent);
    }

    @Override
    protected FunctionCallStreamingResponseHandler fork(FunctionCallStreamingResponseHandler parent) {
        return new JsonschemaFunctionCallStreamingResponseHandler(parent);
    }

    @Override
    protected SseHttpResponse newEmitter(List<ToolExecutionRequest> toolExecutionRequests, Tools.ToolMethod method, Response<AiMessage> response) {
        return null;
    }
}
