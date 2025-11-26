package dev.ai4j.openai4j.chat;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@JsonDeserialize(
        builder = AssistantMessage.Builder.class
)
@JsonInclude(Include.NON_NULL)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public final class AssistantMessage implements Message {
    @JsonProperty
    private final Role role;
    @JsonProperty
    private final String content;
    @JsonProperty
    private final String name;
    @JsonProperty
    private final List<ToolCall> toolCalls;
    @JsonProperty
    private final Boolean refusal;

    @JsonProperty
    @Deprecated
    private final FunctionCall functionCall;
    @JsonProperty
    private final Boolean partial;

    private AssistantMessage(Builder builder) {
        this.role = Role.ASSISTANT;
        this.content = builder.content;
        this.name = builder.name;
        this.toolCalls = builder.toolCalls;
        this.refusal = builder.refusal;
        this.functionCall = builder.functionCall;
        this.partial = builder.partial;
    }

    public static AssistantMessage from(String content) {
        return builder().content(content).build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public Role role() {
        return this.role;
    }

    public Boolean partial() {
        return partial;
    }

    public String content() {
        return this.content;
    }

    public String name() {
        return this.name;
    }

    public List<ToolCall> toolCalls() {
        return this.toolCalls;
    }

    public Boolean refusal() {
        return this.refusal;
    }

    @Deprecated
    public FunctionCall functionCall() {
        return this.functionCall;
    }

    public boolean equals(Object another) {
        if (this == another) {
            return true;
        } else {
            return another instanceof AssistantMessage && this.equalTo((AssistantMessage) another);
        }
    }

    private boolean equalTo(AssistantMessage another) {
        return Objects.equals(this.role, another.role) && Objects.equals(this.content, another.content) && Objects.equals(this.name, another.name) && Objects.equals(this.toolCalls, another.toolCalls) && Objects.equals(this.refusal, another.refusal) && Objects.equals(this.functionCall, another.functionCall);
    }

    public int hashCode() {
        int h = 5381;
        h += (h << 5) + Objects.hashCode(this.role);
        h += (h << 5) + Objects.hashCode(this.content);
        h += (h << 5) + Objects.hashCode(this.name);
        h += (h << 5) + Objects.hashCode(this.toolCalls);
        h += (h << 5) + Objects.hashCode(this.refusal);
        h += (h << 5) + Objects.hashCode(this.functionCall);
        return h;
    }

    public String toString() {
        return "AssistantMessage{role=" + this.role + ", content=" + this.content + ", name=" + this.name + ", toolCalls=" + this.toolCalls + ", refusal=" + this.refusal + ", functionCall=" + this.functionCall + "}";
    }

    @JsonPOJOBuilder(
            withPrefix = ""
    )
    @JsonIgnoreProperties(
            ignoreUnknown = true
    )
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static final class Builder {
        private String content;
        private String name;
        private List<ToolCall> toolCalls;
        private Boolean refusal;
        private Boolean partial;
        @Deprecated
        private FunctionCall functionCall;

        private Builder() {
        }

        public Builder content(String content) {
            this.content = content;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder partial(Boolean partial) {
            this.partial = partial;
            return this;
        }

        public Builder toolCalls(ToolCall... toolCalls) {
            return this.toolCalls(Arrays.asList(toolCalls));
        }

        @JsonSetter
        public Builder toolCalls(List<ToolCall> toolCalls) {
            if (toolCalls != null) {
                this.toolCalls = Collections.unmodifiableList(toolCalls);
            }

            return this;
        }

        public Builder refusal(Boolean refusal) {
            this.refusal = refusal;
            return this;
        }

        @Deprecated
        public Builder functionCall(FunctionCall functionCall) {
            this.functionCall = functionCall;
            return this;
        }

        public AssistantMessage build() {
            return new AssistantMessage(this);
        }
    }
}
