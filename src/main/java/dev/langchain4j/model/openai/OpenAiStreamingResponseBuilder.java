package dev.langchain4j.model.openai;

import com.github.aiassistant.util.AiUtil;
import dev.ai4j.openai4j.chat.*;
import dev.ai4j.openai4j.completion.CompletionChoice;
import dev.ai4j.openai4j.completion.CompletionResponse;
import dev.ai4j.openai4j.shared.Usage;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.output.FinishReason;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.output.TokenUsage;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static dev.langchain4j.model.openai.InternalOpenAiHelper.finishReasonFrom;
import static dev.langchain4j.model.openai.InternalOpenAiHelper.tokenUsageFrom;
import static java.util.stream.Collectors.toList;

/**
 * This class needs to be thread safe because it is called when a streaming result comes back
 * and there is no guarantee that this thread will be the same as the one that initiated the request,
 * in fact it almost certainly won't be.
 */
public class OpenAiStreamingResponseBuilder {

    private final StringBuffer contentBuilder = new StringBuffer();

    private final StringBuffer toolNameBuilder = new StringBuffer();
    private final StringBuffer toolArgumentsBuilder = new StringBuffer();
    // hao, 解决供应商不返回toolCall.index()字段，导致空指针，ConcurrentHashMap改成HashMap
    private final Map<Integer, ToolExecutionRequestBuilder> indexToToolExecutionRequestBuilder = Collections.synchronizedMap(new HashMap<>());

    private volatile TokenUsage tokenUsage;
    private volatile FinishReason finishReason;
    // hotfix: requestID传下来。wangzihao
    private volatile String id;

    public void append(ChatCompletionResponse partialResponse) {
        if (partialResponse == null) {
            return;
        }
        this.id = partialResponse.id();

        Usage usage = partialResponse.usage();
        if (usage != null) {
            this.tokenUsage = tokenUsageFrom(usage);
        }

        List<ChatCompletionChoice> choices = partialResponse.choices();
        if (choices == null || choices.isEmpty()) {
            return;
        }

        ChatCompletionChoice chatCompletionChoice = choices.get(0);
        if (chatCompletionChoice == null) {
            return;
        }

        String finishReason = chatCompletionChoice.finishReason();
        if (finishReason != null) {
            this.finishReason = finishReasonFrom(finishReason);
        }

        Delta delta = chatCompletionChoice.delta();
        if (delta == null) {
            return;
        }

        String content = delta.content();
        if (content != null && !content.isEmpty()) {
            contentBuilder.append(content);
            return;
        }

        if (delta.functionCall() != null) {
            FunctionCall functionCall = delta.functionCall();

            if (functionCall.name() != null) {
                toolNameBuilder.append(functionCall.name());
            }

            if (functionCall.arguments() != null) {
                toolArgumentsBuilder.append(functionCall.arguments());
            }
        }

        if (delta.toolCalls() != null && !delta.toolCalls().isEmpty()) {
            ToolCall toolCall = delta.toolCalls().get(0);

            ToolExecutionRequestBuilder toolExecutionRequestBuilder
                    = indexToToolExecutionRequestBuilder.computeIfAbsent(toolCall.index(), idx -> new ToolExecutionRequestBuilder());

            if (toolCall.id() != null) {
                toolExecutionRequestBuilder.idBuilder.append(toolCall.id());
            }

            FunctionCall functionCall = toolCall.function();

            if (functionCall.name() != null) {
                toolExecutionRequestBuilder.nameBuilder.append(functionCall.name());
            }

            if (functionCall.arguments() != null) {
                toolExecutionRequestBuilder.argumentsBuilder.append(functionCall.arguments());
            }
        }
    }

    public void append(CompletionResponse partialResponse) {
        if (partialResponse == null) {
            return;
        }

        Usage usage = partialResponse.usage();
        if (usage != null) {
            this.tokenUsage = tokenUsageFrom(usage);
        }

        List<CompletionChoice> choices = partialResponse.choices();
        if (choices == null || choices.isEmpty()) {
            return;
        }

        CompletionChoice completionChoice = choices.get(0);
        if (completionChoice == null) {
            return;
        }

        String finishReason = completionChoice.finishReason();
        if (finishReason != null) {
            this.finishReason = finishReasonFrom(finishReason);
        }

        String token = completionChoice.text();
        if (token != null) {
            contentBuilder.append(token);
        }
    }

    public Response<AiMessage> build() {

        String content = contentBuilder.toString();
        if (!content.isEmpty()) {
            return Response.from(
                    AiMessage.from(content),
                    tokenUsage,
                    finishReason,
                    // hotfix: requestID传下来。wangzihao
                    Collections.singletonMap("id", id)
            );
        }

        String toolName = toolNameBuilder.toString();
        if (!toolName.isEmpty()) {
            ToolExecutionRequest toolExecutionRequest = ToolExecutionRequest.builder()
                    .name(toolName)
                    .arguments(toolArgumentsBuilder.toString())
                    .build();
            return Response.from(
                    AiMessage.from(toolExecutionRequest),
                    tokenUsage,
                    finishReason,
                    // hotfix: requestID传下来。wangzihao
                    Collections.singletonMap("id", id)
            );
        }

        if (!indexToToolExecutionRequestBuilder.isEmpty()) {
            List<ToolExecutionRequest> toolExecutionRequests = indexToToolExecutionRequestBuilder.values().stream()
                    .map(it -> ToolExecutionRequest.builder()
                            .id(it.idBuilder.toString())
                            .name(it.nameBuilder.toString())
                            .arguments(it.argumentsBuilder.toString())
                            .build())
                    .collect(toList());
            return Response.from(
                    AiMessage.from(toolExecutionRequests),
                    tokenUsage,
                    finishReason,
                    // hotfix: requestID传下来。wangzihao
                    Collections.singletonMap("id", id)
            );
        }

        // hotfix: 质朴轻言这个供应商的模型有bug，不会回复了。wangzihao
        // https://api-docs.deepseek.com/zh-cn/guides/json_mode,在使用 JSON Output 功能时，API 有概率会返回空的 content。我们正在积极优化该问题，您可以尝试修改 prompt 以缓解此类问题。
        return Response.from(
                AiUtil.NULL,
                tokenUsage,
                finishReason,
                Collections.singletonMap("id", id)
        );
    }

    private static class ToolExecutionRequestBuilder {

        private final StringBuffer idBuilder = new StringBuffer();
        private final StringBuffer nameBuilder = new StringBuffer();
        private final StringBuffer argumentsBuilder = new StringBuffer();
    }
}
