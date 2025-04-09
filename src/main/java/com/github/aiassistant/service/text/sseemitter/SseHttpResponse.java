package com.github.aiassistant.service.text.sseemitter;

/**
 * 给前端推送
 */
public interface SseHttpResponse {
    boolean isEmpty();

    /**
     * 推送部分AI回复
     *
     * @param next 部分AI回复
     */
    default void write(String next) {
        write(new AiMessageString(next));
    }

    void write(AiMessageString next);

    boolean isClose();

    /**
     * 告诉前端结束推送
     */
    void close();

    /**
     * 告诉前端出异常了
     *
     * @param error 异常
     */
    void close(Throwable error);

    default void close(AiMessageString next) {
        write(next);
        close();
    }

    default void close(String next) {
        write(next);
        close();
    }
}