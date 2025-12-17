package com.github.aiassistant.entity.model.langchain4j;

public interface Feature {
    public interface Ignore {
        default boolean isIgnoreAddRepository() {
            return false;
        }
    }
}
