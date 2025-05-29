package com.github.aiassistant.service.text.sseemitter;

/**
 * 给前端推送
 */
public interface SseHttpResponse {
    /**
     * 是否没有推送过内容（即：没调用过 write）
     *
     * @return true=没有推送过内容
     */
    boolean isEmpty();

    /**
     * 立刻发送所有写入的内容
     */
    void flush();

    /**
     * 给前端推送内容
     *
     * @param messageString 推送内容
     */
    default void write(String messageString) {
        write(new AiMessageString(messageString));
    }

    /**
     * 给前端推送内容
     *
     * @param messageString 推送内容
     */
    void write(AiMessageString messageString);

    /**
     * 给前端推送内容（立刻发送）
     *
     * @param messageString 推送内容
     */
    default void writeAndFlush(String messageString) {
        write(new AiMessageString(messageString));
        flush();
    }

    /**
     * 给前端推送内容（立刻发送）
     *
     * @param messageString 推送内容
     */
    default void writeAndFlush(AiMessageString messageString) {
        write(messageString);
        flush();
    }

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