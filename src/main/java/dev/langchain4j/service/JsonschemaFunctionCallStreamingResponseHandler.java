package dev.langchain4j.service;

import com.github.aiassistant.entity.model.chat.QuestionClassifyListVO;
import com.github.aiassistant.service.jsonschema.LlmJsonSchemaApiService;
import com.github.aiassistant.service.text.ChatStreamingResponseHandler;
import com.github.aiassistant.service.text.FunctionCallStreamingResponseHandler;
import com.github.aiassistant.service.text.sseemitter.SseHttpResponse;
import com.github.aiassistant.service.text.tools.Tools;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.model.output.Response;

import java.util.List;
import java.util.concurrent.Executor;

/**
 * Jsonschema工具调用
 */
class JsonschemaFunctionCallStreamingResponseHandler extends FunctionCallStreamingResponseHandler {
    private final Class<?> jsonschemaClass;

    JsonschemaFunctionCallStreamingResponseHandler(String modelName,
                                                   OpenAiStreamingChatModel chatModel, ChatMemory chatMemory,
                                                   ChatStreamingResponseHandler handler,
                                                   LlmJsonSchemaApiService llmJsonSchemaApiService,
                                                   List<Tools.ToolMethod> toolMethodList,
                                                   boolean isSupportChineseToolName,
                                                   int baseMessageIndex, int addMessageCount, Long readTimeoutMs,
                                                   QuestionClassifyListVO classifyListVO,
                                                   Boolean websearch,
                                                   Boolean reasoning,
                                                   Executor executor,
                                                   Class<?> jsonschemaClass) {
        super(modelName, chatModel, chatMemory, handler, llmJsonSchemaApiService,
                toolMethodList, isSupportChineseToolName, baseMessageIndex, addMessageCount, readTimeoutMs, classifyListVO, websearch, reasoning, executor);
        this.jsonschemaClass = jsonschemaClass;
    }

    JsonschemaFunctionCallStreamingResponseHandler(JsonschemaFunctionCallStreamingResponseHandler parent) {
        super(parent);
        this.jsonschemaClass = parent.jsonschemaClass;
    }

    @Override
    protected FunctionCallStreamingResponseHandler copy(FunctionCallStreamingResponseHandler parent) {
        return new JsonschemaFunctionCallStreamingResponseHandler((JsonschemaFunctionCallStreamingResponseHandler) parent);
    }

    @Override
    protected SseHttpResponse newEmitter(List<ToolExecutionRequest> toolExecutionRequests, Tools.ToolMethod method, Response<AiMessage> response) {
        return null;
    }

    @Override
    public String name() {
        return "JsonschemaFunctionCallStreamingResponseHandler{" + getModelName() + ", " + jsonschemaClass.getSimpleName() + "}";
    }
}
