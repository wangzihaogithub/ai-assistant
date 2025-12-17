package com.github.aiassistant.entity.model.langchain4j;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.Content;

import java.util.List;

public interface LangChain extends ChatMessage {

    public static class AiMessage extends dev.langchain4j.data.message.AiMessage implements LangChain, Feature.Ignore {
        private final boolean ignoreAddRepository;

        public AiMessage(String text, boolean ignoreAddRepository) {
            super(text);
            this.ignoreAddRepository = ignoreAddRepository;
        }

        public AiMessage(List<ToolExecutionRequest> toolExecutionRequests, boolean ignoreAddRepository) {
            super(toolExecutionRequests);
            this.ignoreAddRepository = ignoreAddRepository;
        }

        public AiMessage(String text, List<ToolExecutionRequest> toolExecutionRequests, boolean ignoreAddRepository) {
            super(text, toolExecutionRequests);
            this.ignoreAddRepository = ignoreAddRepository;
        }

        @Override
        public boolean isIgnoreAddRepository() {
            return ignoreAddRepository;
        }

    }

    public static class UserMessage extends dev.langchain4j.data.message.UserMessage implements LangChain, Feature.Ignore {
        private final boolean ignoreAddRepository;

        public UserMessage(String text, boolean ignoreAddRepository) {
            super(text);
            this.ignoreAddRepository = ignoreAddRepository;
        }

        public UserMessage(String name, List<Content> contents, boolean ignoreAddRepository) {
            super(name, contents);
            this.ignoreAddRepository = ignoreAddRepository;
        }

        public UserMessage(List<Content> contents, boolean ignoreAddRepository) {
            super(contents);
            this.ignoreAddRepository = ignoreAddRepository;
        }

        @Override
        public boolean isIgnoreAddRepository() {
            return ignoreAddRepository;
        }

    }

}
