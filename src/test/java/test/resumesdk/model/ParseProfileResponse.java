package test.resumesdk.model;

import org.springframework.http.HttpHeaders;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 简历画像
 * ResumeSDK简历画像提供对输入的附件简历进行提取并进行人才画像的功能。
 * 接口文档：https://www.resumesdk.com/docs/rs-profiler.html#reqUrl
 * <p>
 * 接口格式
 * 简历画像的输入为附件简历，其请求接口格式和简历解析完全一致，参考 简历解析请求接口 ；
 * 调用时仅需将请求url替换成上面简历画像对应的接口url；
 * <p>
 * status：请求结果状态信息（简历解析对应字段）；
 * account：账户状态信息（简历解析对应字段）；
 * result：简历解析结果信息（简历解析对应字段）；
 * profiler_result：简历画像结果信息（简历画像新增字段）；
 * ResumeSDK以json格式返回上述信息，其中profiler_result为简历画像独有字段，其他字段和简历解析一致。
 */
public class ParseProfileResponse {
    private final Integer statusCode;
    private final Map<String, List<String>> headers;
    private final Map<String, Object> body;

    public ParseProfileResponse(Integer statusCode, HttpHeaders headers, Map<String, Object> body) {
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