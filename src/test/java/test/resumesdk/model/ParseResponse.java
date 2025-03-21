package test.resumesdk.model;

import org.springframework.http.HttpHeaders;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 简历解析结果均放在result字段下，共包含170多个字段信息。
 * 接口文档：https://www.resumesdk.com/docs/rs-parser.html#
 */
public class ParseResponse {
    private final Integer statusCode;
    private final Map<String, List<String>> headers;
    private final Map<String, Object> body;

    public ParseResponse(Integer statusCode, HttpHeaders headers, Map<String, Object> body) {
        this.statusCode = statusCode;
        this.headers = new LinkedHashMap<>(headers);
        this.body = body;
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    public Map<String, List<String>> getHeaders() {
        return headers;
    }

    public Map<String, Object> getBody() {
        return body;
    }
}