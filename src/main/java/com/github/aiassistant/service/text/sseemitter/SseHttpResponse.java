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
    void write(String next);

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

    default void close(String next) {
        write(next);
        close();
    }
}