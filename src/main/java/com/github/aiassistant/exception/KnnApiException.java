package com.github.aiassistant.exception;

/**
 * RAG相似搜索接口错误
 */
public class KnnApiException extends AiAssistantException {
    private final String indexName;
    private final byte[] requestBody;

    public KnnApiException(String message, Throwable cause, String indexName, byte[] requestBody) {
        super(message, cause);
        this.indexName = indexName;
        this.requestBody = requestBody;
    }

    public String getIndexName() {
        return indexName;
    }

    public byte[] getRequestBody() {
        return requestBody;
    }
}
