package com.github.aiassistant.service.text.sseemitter;

/**
 * 给前端推送
 */
public interface SseHttpResponse {
    boolean isEmpty();

    void write(String next);

    void close();

    void close(Throwable error);

    default void close(String next) {
        write(next);
        close();
    }
}