package dev.ai4j.openai4j.chat;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import java.util.List;
import java.util.Objects;

import static java.util.Collections.unmodifiableList;

@JsonDeserialize(builder = Delta.Builder.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public final class Delta {

    @JsonProperty
    private final Role role;
    @JsonProperty
    private final String content;
    @JsonProperty
    private final String reasoningContent;
    @JsonProperty
    private final List<ToolCall> toolCalls;
    @JsonProperty
    @Deprecated
    private final FunctionCall functionCall;
    @JsonProperty
    private final Audio audio;

    private Delta(Builder builder) {
        this.role = builder.role;
        this.content = builder.content;
        this.reasoningContent = builder.reasoningContent;
        this.toolCalls = builder.toolCalls;
        this.functionCall = builder.functionCall;
        this.audio = builder.audio;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Audio audio() {
        return audio;
    }

    public Role role() {
        return role;
    }

    public String content() {
        return content;
    }

    public String reasoningContent() {
        return reasoningContent;
    }

    public List<ToolCall> toolCalls() {
        return toolCalls;
    }

    @Deprecated
    public FunctionCall functionCall() {
        return functionCall;
    }

    @Override
    public boolean equals(Object another) {
        if (this == another) return true;
        return another instanceof Delta
                && equalTo((Delta) another);
    }

    private boolean equalTo(Delta another) {
        return Objects.equals(role, another.role)
                && Objects.equals(content, another.content)
                && Objects.equals(toolCalls, another.toolCalls)
                && Objects.equals(reasoningContent, another.reasoningContent)
                && Objects.equals(audio, another.audio)
                && Objects.equals(functionCall, another.functionCall);
    }

    @Override
    public int hashCode() {
        int h = 5381;
        h += (h << 5) + Objects.hashCode(role);
        h += (h << 5) + Objects.hashCode(content);
        h += (h << 5) + Objects.hashCode(toolCalls);
        h += (h << 5) + Objects.hashCode(reasoningContent);
        h += (h << 5) + Objects.hashCode(audio);
        h += (h << 5) + Objects.hashCode(functionCall);
        return h;
    }

    @Override
    public String toString() {
        return "Delta{"
                + "role=" + role
                + ", content=" + content
                + ", toolCalls=" + toolCalls
                + ", functionCall=" + functionCall
                + "}";
    }

    @JsonPOJOBuilder(withPrefix = "")
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static final class Builder {

        private Role role;
        private String content;
        private String reasoningContent;
        private List<ToolCall> toolCalls;
        @Deprecated
        private FunctionCall functionCall;
        private Audio audio;

        private Builder() {
        }

        public Builder audio(Audio audio) {
            this.audio = audio;
            return this;
        }

        public Builder role(Role role) {
            this.role = role;
            return this;
        }

        public Builder content(String content) {
            this.content = content;
            return this;
        }

        public Builder reasoningContent(String reasoningContent) {
            this.reasoningContent = reasoningContent;
            return this;
        }

        public Builder toolCalls(List<ToolCall> toolCalls) {
            if (toolCalls != null) {
                this.toolCalls = unmodifiableList(toolCalls);
            }
            return this;
        }

        @Deprecated
        public Builder functionCall(FunctionCall functionCall) {
            this.functionCall = functionCall;
            return this;
        }


        public Delta build() {
            return new Delta(this);
        }
    }

    public static class Audio {
        /**
         * wav_bytes = base64.b64decode(audio_string)
         * audio_np = np.frombuffer(wav_bytes, dtype=np.int16)
         * sf.write("audio_assistant_py.wav", audio_np, samplerate=24000)
         */
        private String data;
        private String transcript;

        public String getData() {
            return data;
        }

        public void setData(String data) {
            this.data = data;
        }

        public String getTranscript() {
            return transcript;
        }

        public void setTranscript(String transcript) {
            this.transcript = transcript;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Audio)) return false;
            Audio audio = (Audio) o;
            return Objects.equals(data, audio.data) && Objects.equals(transcript, audio.transcript);
        }

        @Override
        public int hashCode() {
            return Objects.hash(data, transcript);
        }
    }

}
