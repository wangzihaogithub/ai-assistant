package test.resumesdk;


import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import test.resumesdk.model.ParseProfileResponse;
import test.resumesdk.model.ParseRequest;
import test.resumesdk.model.ParseResponse;

import java.util.Collections;
import java.util.Map;

/**
 * 招聘行业解析工具
 * 官网：https://www.resumesdk.com/
 */
@Component
public class ResumeSdkApiService {
    private final RestTemplate restTemplate = new RestTemplate();
    private final HttpHeaders requestHeaders = new HttpHeaders();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private String profileUrl = "http://www.resumesdk.com/api/parse_profile";
    private String parseUrl = "http://www.resumesdk.com/api/parse";

    public ResumeSdkApiService() {
        this.requestHeaders.setContentType(MediaType.APPLICATION_JSON);
    }

    public void setParseUrl(String parseUrl) {
        this.parseUrl = parseUrl;
    }

    public void setProfileUrl(String profileUrl) {
        this.profileUrl = profileUrl;
    }

    public void setUid(String uid) {
        this.requestHeaders.set("uid", uid);
    }

    public void setPwd(String pwd) {
        this.requestHeaders.set("pwd", pwd);
    }

    /**
     * 简历画像
     * ResumeSDK简历画像提供对输入的附件简历进行提取并进行人才画像的功能。
     * 接口文档：https://www.resumesdk.com/docs/rs-profiler.html#reqUrl
     * 注意事项
     * 简历画像功能，需要先对简历进行解析，在解析结果的基础上进行画像。因此接口返回结果同时包含简历解析和简历画像的结果；
     * 不定期新增字段：功能升级需要，我们可能会不定期在json结果中增加一些新的字段，因此在读取结果数据时，请注意对后续新增字段的兼容；
     * <p>
     * 接口格式
     * 简历画像的输入为附件简历，其请求接口格式和简历解析完全一致，参考 简历解析请求接口 ；
     * 调用时仅需将请求url替换成上面简历画像对应的接口url；
     */
    public ParseProfileResponse profile(ParseRequest parseRequest) {
        return restTemplate.execute(profileUrl,
                HttpMethod.POST,
                request -> {
                    request.getHeaders().putAll(requestHeaders);
                    objectMapper.writeValue(request.getBody(), parseRequest.toRequestBody());
                }, response -> {
                    Map body = objectMapper.readValue(response.getBody(), Map.class);
                    return new ParseProfileResponse(response.getRawStatusCode(), response.getHeaders(), body);
                }, Collections.emptyMap());
    }

    /**
     * 简历解析
     * ResumeSDK简历解析提供对输入的附件简历进行结构化处理的功能，共解析出10大主要功能模块，合计170多个字段信息。
     * 接口文档：https://www.resumesdk.com/docs/rs-parser.html#reqType
     */
    public ParseResponse parse(ParseRequest parseRequest) {
        return restTemplate.execute(parseUrl,
                HttpMethod.POST,
                request -> {
                    request.getHeaders().putAll(requestHeaders);
                    objectMapper.writeValue(request.getBody(), parseRequest.toRequestBody());
                }, response -> {
                    Map body = objectMapper.readValue(response.getBody(), Map.class);
                    return new ParseResponse(response.getRawStatusCode(), response.getHeaders(), body);
                }, Collections.emptyMap());
    }

}
