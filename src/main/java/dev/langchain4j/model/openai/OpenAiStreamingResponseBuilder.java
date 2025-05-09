package dev.langchain4j.model.openai;

import com.github.aiassistant.entity.model.langchain4j.ThinkingAiMessage;
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

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static dev.langchain4j.model.openai.InternalOpenAiHelper.finishReasonFrom;
import static dev.langchain4j.model.openai.InternalOpenAiHelper.tokenUsageFrom;
import static java.util.stream.Collectors.toList;

/**
 * This class needs to be thread safe because it is called when a streaming result comes back
 * and there is no guarantee that this thread will be the same as the one that initiated the request,
 * in fact it almost certainly won't be.
 */
public class OpenAiStreamingResponseBuilder {

    public static int STATE_THINKING = 1;
    public static int STATE_OUTPUT = 2;
    private final StringBuffer contentBuilder = new StringBuffer();
    private final StringBuffer reasoningContentBuilder = new StringBuffer();
    private final StringBuffer toolNameBuilder = new StringBuffer();
    private final StringBuffer toolArgumentsBuilder = new StringBuffer();
    // hao, 解决供应商不返回toolCall.index()字段，导致空指针，ConcurrentHashMap改成HashMap
    private final Map<Integer, ToolExecutionRequestBuilder> indexToToolExecutionRequestBuilder = Collections.synchronizedMap(new HashMap<>());
    private final AtomicInteger state = new AtomicInteger(STATE_OUTPUT);
    private volatile PipedInputStream audioBuilderInput;
    private volatile PipedOutputStream audioBuilderOutput;
    private volatile TokenUsage tokenUsage;
    private volatile FinishReason finishReason;
    // hotfix: requestID传下来。wangzihao
    private volatile String id;

    private static AiMessage buildAiMessage(
            int state,
            StringBuffer contentBuilder,
            StringBuffer reasoningContentBuilder,
            List<ToolExecutionRequest> toolExecutionRequests) {
        if (state == STATE_THINKING) {
            String contentString = reasoningContentBuilder.toString();
            reasoningContentBuilder.setLength(0);
            if (contentString.isEmpty() && toolExecutionRequests == null) {
                return AiUtil.NULL;
            } else if (contentString.isEmpty()) {
                return new ThinkingAiMessage(toolExecutionRequests);
            } else if (toolExecutionRequests == null) {
                return new ThinkingAiMessage(contentString);
            } else {
                return new ThinkingAiMessage(contentString, toolExecutionRequests);
            }
        } else {
            // reasoning call tool
            boolean reasoning;
            StringBuffer builder;
            if (contentBuilder.length() > 0) {
                builder = contentBuilder;
                reasoning = false;
            } else {
                builder = reasoningContentBuilder;
                reasoning = true;
            }
            String contentString = builder.toString();
            builder.setLength(0);
            if (contentString.isEmpty() && toolExecutionRequests == null) {
                // hotfix: 质朴轻言这个供应商的模型有bug，不会回复了。wangzihao
                // https://api-docs.deepseek.com/zh-cn/guides/json_mode,在使用 JSON Output 功能时，API 有概率会返回空的 content。我们正在积极优化该问题，您可以尝试修改 prompt 以缓解此类问题。
                return AiUtil.NULL;
            } else {
                if (reasoning) {
                    if (contentString.isEmpty()) {
                        return new ThinkingAiMessage(toolExecutionRequests);
                    } else if (toolExecutionRequests == null) {
                        return new ThinkingAiMessage(contentString);
                    } else {
                        return new ThinkingAiMessage(contentString, toolExecutionRequests);
                    }
                } else {
                    if (contentString.isEmpty()) {
                        return new AiMessage(toolExecutionRequests);
                    } else if (toolExecutionRequests == null) {
                        return new AiMessage(contentString);
                    } else {
                        return new AiMessage(contentString, toolExecutionRequests);
                    }
                }
            }
        }
    }

    public boolean compareAndSet(int expect, int update) {
        return state.compareAndSet(expect, update);
    }

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

        Delta.Audio audio = delta.audio();
        if (audio != null) {
            String data = audio.getData();
            if (data != null && !data.isEmpty()) {
                initAudioBuilderIfNeed();
                byte[] decode = Base64.getDecoder().decode(data);
                try {
                    audioBuilderOutput.write(decode);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        String reasoningContent = delta.reasoningContent();
        if (reasoningContent != null && !reasoningContent.isEmpty()) {
            reasoningContentBuilder.append(reasoningContent);
        }

        String content = delta.content();
        if (content != null && !content.isEmpty()) {
            contentBuilder.append(content);
        }

        FunctionCall functionCall = delta.functionCall();
        if (functionCall != null) {
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

            FunctionCall functionCall1 = toolCall.function();
            if (functionCall1.name() != null) {
                toolExecutionRequestBuilder.nameBuilder.append(functionCall1.name());
            }

            if (functionCall1.arguments() != null) {
                toolExecutionRequestBuilder.argumentsBuilder.append(functionCall1.arguments());
            }
        }
    }

    public PipedInputStream getAudioBuilderInput() {
        return audioBuilderInput;
    }

    private void initAudioBuilderIfNeed() {
        if (audioBuilderInput == null) {
            synchronized (this) {
                if (audioBuilderInput == null) {
                    audioBuilderInput = new PipedInputStream(4096);
                    try {
                        audioBuilderOutput = new PipedOutputStream(audioBuilderInput);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
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
        return build(STATE_OUTPUT);
    }

    public Response<AiMessage> build(int state) {
        List<ToolExecutionRequest> toolExecutionRequests;
        if (toolNameBuilder.length() > 0) {
            ToolExecutionRequest toolExecutionRequest = ToolExecutionRequest.builder()
                    .name(toolNameBuilder.toString())
                    .arguments(toolArgumentsBuilder.toString())
                    .build();
            toolExecutionRequests = Collections.singletonList(toolExecutionRequest);
        } else if (!indexToToolExecutionRequestBuilder.isEmpty()) {
            toolExecutionRequests = indexToToolExecutionRequestBuilder.values().stream()
                    .map(it -> ToolExecutionRequest.builder()
                            .id(it.idBuilder.toString())
                            .name(it.nameBuilder.toString())
                            .arguments(it.argumentsBuilder.toString())
                            .build())
                    .collect(toList());
        } else {
            toolExecutionRequests = null;
        }
        return new Response<>(
                buildAiMessage(state, contentBuilder, reasoningContentBuilder, toolExecutionRequests),
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
