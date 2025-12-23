package com.github.aiassistant.service.text.sseemitter;

import com.github.aiassistant.enums.UserTriggerEventEnum;

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
     * 给前端推送用户触发事件
     *
     * @param triggerEventEnum 事件类型
     * @param payload          事件数据
     * @param timestamp        事件时间
     * @param <T>              事件数据类型
     */
    <T> void userTrigger(UserTriggerEventEnum<T> triggerEventEnum, T payload, long timestamp);

    /**
     * 给前端推送用户触发事件（默认事件时间为当前时间）
     *
     * @param triggerEventEnum 事件类型
     * @param payload          事件数据
     * @param <T>              事件数据类型
     */
    default <T> void userTrigger(UserTriggerEventEnum<T> triggerEventEnum, T payload) {
        userTrigger(triggerEventEnum, payload, System.currentTimeMillis());
    }

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