package com.github.aiassistant.exception;

/**
 * 大模型token返回超时
 */
public class TokenReadTimeoutException extends AiAssistantException {

    private final long timeout;
    private final long readTimeoutMs;

    public TokenReadTimeoutException(String message, long timeout, long readTimeoutMs) {
        super(message);
        this.timeout = timeout;
        this.readTimeoutMs = readTimeoutMs;
    }

    public long getTimeout() {
        return timeout;
    }

    public long getReadTimeoutMs() {
        return readTimeoutMs;
    }
}
