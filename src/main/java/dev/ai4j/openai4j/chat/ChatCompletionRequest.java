package dev.ai4j.openai4j.chat;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import dev.ai4j.openai4j.shared.StreamOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static dev.ai4j.openai4j.chat.ChatCompletionModel.GPT_3_5_TURBO;
import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableMap;

@JsonDeserialize(builder = ChatCompletionRequest.Builder.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public final class ChatCompletionRequest {

    @JsonProperty
    private final String model;
    @JsonProperty
    private final List<Message> messages;
    @JsonProperty
    private final Double temperature;
    @JsonProperty
    private final Double topP;
    @JsonProperty
    private final Integer n;
    @JsonProperty
    private final Boolean stream;
    @JsonProperty
    private final StreamOptions streamOptions;
    @JsonProperty
    private final List<String> stop;
    @JsonProperty
    private final Integer maxCompletionTokens;
    @JsonProperty
    private final Integer maxTokens;
    @JsonProperty
    private final Double presencePenalty;
    @JsonProperty
    private final Double frequencyPenalty;
    @JsonProperty
    private final Map<String, Integer> logitBias;
    @JsonProperty
    private final String user;
    @JsonProperty
    private final ResponseFormat responseFormat;
    @JsonProperty
    private final Integer seed;
    @JsonProperty
    private final List<Tool> tools;
    @JsonProperty
    private final Object toolChoice;
    @JsonProperty
    private final Boolean parallelToolCalls;
    @JsonProperty
    private final Boolean enableThinking;
    @JsonProperty
    private final List<String> modalities;
    @JsonProperty
    private final Boolean enableSearch;
    @JsonProperty
    private final Integer thinkingBudget;
    @JsonProperty
    private final Map<String, Object> searchOptions;
    @JsonProperty
    private final Audio audio;

    private ChatCompletionRequest(Builder builder) {
        this.model = builder.model;
        this.messages = builder.messages;
        this.temperature = builder.temperature;
        this.topP = builder.topP;
        this.n = builder.n;
        this.stream = builder.stream;
        this.streamOptions = builder.streamOptions;
        this.stop = builder.stop;
        this.maxCompletionTokens = builder.maxCompletionTokens;
        this.maxTokens = builder.maxCompletionTokens;
        this.presencePenalty = builder.presencePenalty;
        this.frequencyPenalty = builder.frequencyPenalty;
        this.logitBias = builder.logitBias;
        this.user = builder.user;
        this.responseFormat = builder.responseFormat;
        this.seed = builder.seed;
        this.tools = builder.tools;
        this.toolChoice = builder.toolChoice;
        this.parallelToolCalls = builder.parallelToolCalls;
        this.enableThinking = builder.enableThinking;
        this.modalities = builder.modalities;
        this.enableSearch = builder.enableSearch;
        this.thinkingBudget = builder.thinkingBudget;
        this.searchOptions = builder.searchOptions;
        this.audio = builder.audio;
    }

    public static Builder builder() {
        return new Builder();
    }

    public List<String> modalities() {
        return modalities;
    }

    public Integer thinkingBudget() {
        return thinkingBudget;
    }

    public Boolean enableThinking() {
        return enableThinking;
    }

    public Boolean enableSearch() {
        return enableSearch;
    }

    public String model() {
        return model;
    }

    public List<Message> messages() {
        return messages;
    }

    public Double temperature() {
        return temperature;
    }

    public Double topP() {
        return topP;
    }

    public Integer n() {
        return n;
    }

    public Boolean stream() {
        return stream;
    }

    public StreamOptions streamOptions() {
        return streamOptions;
    }

    public List<String> stop() {
        return stop;
    }

    public Integer maxCompletionTokens() {
        return maxCompletionTokens;
    }

    public Integer maxTokens() {
        return maxTokens;
    }

    public Double presencePenalty() {
        return presencePenalty;
    }

    public Double frequencyPenalty() {
        return frequencyPenalty;
    }

    public Map<String, Integer> logitBias() {
        return logitBias;
    }

    public String user() {
        return user;
    }

    public ResponseFormat responseFormat() {
        return responseFormat;
    }

    public Integer seed() {
        return seed;
    }

    public List<Tool> tools() {
        return tools;
    }

    public Object toolChoice() {
        return toolChoice;
    }

    public Boolean parallelToolCalls() {
        return parallelToolCalls;
    }

    @JsonPOJOBuilder(withPrefix = "")
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static final class Builder {

        private String model = GPT_3_5_TURBO.toString();
        private List<Message> messages;
        private Double temperature;
        private Double topP;
        private Integer n;
        private Boolean stream;
        private StreamOptions streamOptions;
        private List<String> stop;
        private Integer maxCompletionTokens;
        private Double presencePenalty;
        private Double frequencyPenalty;
        private Map<String, Integer> logitBias;
        private String user;
        private ResponseFormat responseFormat;
        private Integer seed;
        private List<Tool> tools;
        private Object toolChoice;
        private Boolean parallelToolCalls;
        private Boolean enableThinking;
        private Boolean enableSearch;
        private Integer thinkingBudget;
        private List<String> modalities;
        private Map<String, Object> searchOptions;
        private Audio audio;
        private Builder() {
        }

        /**
         * 保留这个方法，openai4j其他包会调用
         *
         * @param instance instance
         * @return Builder
         */
        public Builder from(ChatCompletionRequest instance) {
            model(instance.model);
            messages(instance.messages);
            temperature(instance.temperature);
            topP(instance.topP);
            n(instance.n);
            stream(instance.stream);
            streamOptions(instance.streamOptions);
            stop(instance.stop);
            maxCompletionTokens(instance.maxCompletionTokens);
            presencePenalty(instance.presencePenalty);
            frequencyPenalty(instance.frequencyPenalty);
            logitBias(instance.logitBias);
            user(instance.user);
            responseFormat(instance.responseFormat);
            seed(instance.seed);
            tools(instance.tools);
            toolChoice(instance.toolChoice);
            parallelToolCalls(instance.parallelToolCalls);
            enableThinking(instance.enableThinking);
            modalities(instance.modalities);
            enableSearch(instance.enableSearch);
            thinkingBudget(instance.thinkingBudget);
            searchOptions(instance.searchOptions);
            audio(instance.audio);
            return this;
        }

        public Builder audio(Audio audio) {
            this.audio = audio;
            return this;
        }

        public Builder searchOptions(Map<String, Object> searchOptions) {
            this.searchOptions = searchOptions;
            return this;
        }

        public Builder thinkingBudget(Integer thinkingBudget) {
            this.thinkingBudget = thinkingBudget;
            return this;
        }

        public Builder model(String model) {
            this.model = model;
            return this;
        }

        public Builder model(ChatCompletionModel model) {
            return model(model.toString());
        }

        @JsonSetter
        public Builder messages(List<Message> messages) {
            if (messages != null) {
                this.messages = unmodifiableList(messages);
            }
            return this;
        }

        public Builder messages(Message... messages) {
            return messages(asList(messages));
        }

        public Builder addSystemMessage(String systemMessage) {
            if (this.messages == null) {
                this.messages = new ArrayList<>();
            }
            this.messages.add(SystemMessage.from(systemMessage));
            return this;
        }

        public Builder addUserMessage(String userMessage) {
            if (this.messages == null) {
                this.messages = new ArrayList<>();
            }
            this.messages.add(UserMessage.from(userMessage));
            return this;
        }

        public Builder addAssistantMessage(String assistantMessage) {
            if (this.messages == null) {
                this.messages = new ArrayList<>();
            }
            this.messages.add(AssistantMessage.from(assistantMessage));
            return this;
        }

        public Builder addToolMessage(String toolCallId, String content) {
            if (this.messages == null) {
                this.messages = new ArrayList<>();
            }
            this.messages.add(ToolMessage.from(toolCallId, content));
            return this;
        }

        public Builder temperature(Double temperature) {
            this.temperature = temperature;
            return this;
        }

        public Builder enableSearch(Boolean enableSearch) {
            this.enableSearch = enableSearch;
            return this;
        }

        public Builder topP(Double topP) {
            this.topP = topP;
            return this;
        }

        public Builder n(Integer n) {
            this.n = n;
            return this;
        }

        public Builder stream(Boolean stream) {
            this.stream = stream;
            return this;
        }

        public Builder streamOptions(StreamOptions streamOptions) {
            this.streamOptions = streamOptions;
            return this;
        }

        @JsonSetter
        public Builder stop(List<String> stop) {
            if (stop != null) {
                this.stop = unmodifiableList(stop);
            }
            return this;
        }

        public Builder stop(String... stop) {
            return stop(asList(stop));
        }

        public Builder maxCompletionTokens(Integer maxCompletionTokens) {
            this.maxCompletionTokens = maxCompletionTokens;
            return this;
        }

        public Builder presencePenalty(Double presencePenalty) {
            this.presencePenalty = presencePenalty;
            return this;
        }

        public Builder frequencyPenalty(Double frequencyPenalty) {
            this.frequencyPenalty = frequencyPenalty;
            return this;
        }

        public Builder logitBias(Map<String, Integer> logitBias) {
            if (logitBias != null) {
                this.logitBias = unmodifiableMap(logitBias);
            }
            return this;
        }

        public Builder user(String user) {
            this.user = user;
            return this;
        }

        public Builder responseFormat(ResponseFormatType responseFormatType) {
            if (responseFormatType != null) {
                responseFormat = ResponseFormat.builder()
                        .type(responseFormatType)
                        .build();
            }
            return this;
        }

        @JsonSetter
        public Builder responseFormat(ResponseFormat responseFormat) {
            this.responseFormat = responseFormat;
            return this;
        }

        public Builder seed(Integer seed) {
            this.seed = seed;
            return this;
        }

        @JsonSetter
        public Builder tools(List<Tool> tools) {
            if (tools != null) {
                this.tools = unmodifiableList(tools);
            }
            return this;
        }

        public Builder tools(Tool... tools) {
            return tools(asList(tools));
        }

        public Builder toolChoice(ToolChoiceMode toolChoiceMode) {
            this.toolChoice = toolChoiceMode;
            return this;
        }

        public Builder toolChoice(String functionName) {
            return toolChoice(ToolChoice.from(functionName));
        }

        public Builder toolChoice(Object toolChoice) {
            this.toolChoice = toolChoice;
            return this;
        }

        public Builder parallelToolCalls(Boolean parallelToolCalls) {
            this.parallelToolCalls = parallelToolCalls;
            return this;
        }

        public Builder enableThinking(Boolean enableThinking) {
            this.enableThinking = enableThinking;
            return this;
        }

        public Builder modalities(List<String> modalities) {
            this.modalities = modalities;
            return this;
        }

        public ChatCompletionRequest build() {
            return new ChatCompletionRequest(this);
        }
    }
}
