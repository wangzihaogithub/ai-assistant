package com.github.aiassistant.service.text.tools.functioncall;

import com.github.aiassistant.entity.model.chat.UrlReadToolExecutionResultMessage;
import com.github.aiassistant.platform.ApacheHttpClient;
import com.github.aiassistant.platform.HtmlQuery;
import com.github.aiassistant.platform.PlatformDependentUtil;
import com.github.aiassistant.service.text.tools.Tools;
import com.github.aiassistant.util.*;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolMemoryId;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.zip.DeflaterInputStream;
import java.util.zip.GZIPInputStream;

/**
 * 链接读取
 */
public class UrlReadTools extends Tools {
    public static final String NO_PROXY = "NO_PROXY";
    private static final UserAgentGenerator userAgentGenerator = new UserAgentGenerator();
    public static Proxy PROXY1 = null;
    public static Proxy PROXY2 = null;
    private final int max302;
    private final String[] headers = {
            "accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7",
            "accept-language", "zh-CN,zh;q=0.9",
            "accept-encoding", "gzip",
            "cache-control", "no-cache",
            "sec-ch-ua", "\"Google Chrome\";v=\"131\", \"Chromium\";v=\"131\", \"Not_A Brand\";v=\"24\"",
            "sec-ch-ua-mobile", "?0",
            "sec-ch-ua-platform", "\"macOS\"",
            "sec-fetch-dest", "document",
            "sec-fetch-mode", "navigate",
            "sec-fetch-site", "none",
            "sec-fetch-user", "?1",
            "upgrade-insecure-requests", "1",
            "pragma", "no-cache",
//            "user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"
    };
    private final HttpClient[] httpClients;
    private final Proxy proxy;
    private int httpClientsIndex;

    public UrlReadTools(String threadNamePrefix, int connectTimeout, int readTimeout, int max302) {
        this(threadNamePrefix, 6, connectTimeout, readTimeout, max302, null);
    }

    public UrlReadTools(String threadNamePrefix, int connectTimeout, int readTimeout, int max302,
                        Proxy proxy) {
        this(threadNamePrefix, 6, connectTimeout, readTimeout, max302, proxy);
    }

    public UrlReadTools(String threadNamePrefix, int clients,
                        int connectTimeout, int readTimeout, int max302,
                        Proxy proxy) {
        this.max302 = max302;
        this.httpClients = new HttpClient[clients];
        for (int i = 0; i < httpClients.length; i++) {
            HttpClient client;
            if (PlatformDependentUtil.isSupportApacheHttpClient()) {
                client = new ApacheHttpClient(threadNamePrefix);
            } else {
                client = new JdkHttpClient();
            }
            client.setReadTimeout(readTimeout);
            client.setConnectTimeout(connectTimeout);
            client.ignoreHttpsValidation();
            if (proxy != null) {
                client.setProxy(proxy);
            }
            httpClients[i] = client;
        }
        this.proxy = proxy;
    }

    private static ByteArrayOutputStream toByteArrayOutStream(InputStream source)
            throws IOException {
        int available = Math.max(source.available(), 8192);
        ByteArrayOutputStream sink = new ByteArrayOutputStream();
        byte[] buf = new byte[available];
        int n;
        while ((n = source.read(buf)) > 0) {
            sink.write(buf, 0, n);
        }
        return sink;
    }

    public ProxyVO getProxyVO() {
        if (proxy != null && proxy.address() instanceof InetSocketAddress) {
            InetSocketAddress address = (InetSocketAddress) proxy.address();
            return new ProxyVO(address.getHostString(), address.getPort());
        }
        return null;
    }

    private CompletableFuture<String> readString(String uriTemplate, Map<String, ?> uriVariables, String[] headers,
                                                 int max302) {
        CompletableFuture<Object> read = read(uriTemplate, uriVariables, headers, max302);
        return read.thenApply(o -> {
            if (o instanceof HtmlQuery) {
                return ((HtmlQuery<?>) o).text();
            } else {
                return Objects.toString(o, null);
            }
        });
    }

    public CompletableFuture<Object> read(String uriTemplate, Map<String, ?> uriVariables, String[] headers,
                                          int max302) {
        HttpClient client = httpClients[httpClientsIndex++ % httpClients.length];
        HttpClient.HttpRequest connection;
        try {
            connection = client.request(uriTemplate, uriVariables);
        } catch (IOException e) {
            CompletableFuture<Object> f = new CompletableFuture<>();
            f.completeExceptionally(e);
            return f;
        }
        connection.setHeader("user-agent", userAgentGenerator.generateUserAgent());
        for (int i = 0; i < headers.length; i += 2) {
            connection.setHeader(headers[i], headers[i + 1]);
        }
        CompletableFuture<CompletableFuture<Object>> future = connection.connect().thenApply(response -> {
            try {
                String contentTypeString = response.getHeader("content-type");
                ContentType contentType = contentTypeString != null ? ContentType.parse(contentTypeString) : ContentType.create("text/html", "utf-8");
                String charset = null;
                if (contentType != null) {
                    charset = contentType.getCharset();
                }
                if (charset == null) {
                    charset = "utf-8";
                }
                String location = response.getHeader("Location");
                if (max302 > 0 && location != null && !location.isEmpty()) {
                    return read(location, Collections.emptyMap(), headers, max302 - 1);
                }
                String contentEncoding = response.getHeader("Content-Encoding");
                InputStream inputStream;
                if (contentEncoding != null && contentEncoding.toLowerCase().contains("gzip")) {
                    inputStream = new GZIPInputStream(response.getInputStream());
                } else if (contentEncoding != null && contentEncoding.toLowerCase().contains("deflate")) {
                    inputStream = new DeflaterInputStream(response.getInputStream());
                } else {
                    inputStream = response.getInputStream();
                }

                ByteArrayOutputStream outStream = toByteArrayOutStream(inputStream);
                String bodyString = outStream.toString(charset);
                if (!StringUtils.hasText(bodyString)) {
                    return CompletableFuture.completedFuture("");
                } else if (PlatformDependentUtil.isSupportJsoup()) {
                    return CompletableFuture.completedFuture(
                            HtmlQuery.valueOfContentType(bodyString, contentType, outStream));
                } else {
                    return CompletableFuture.completedFuture(bodyString);
                }
            } catch (Exception e) {
                CompletableFuture<Object> f = new CompletableFuture<>();
                f.completeExceptionally(e);
                return f;
            }
        });
        return FutureUtil.allOf(future);
    }

    public CompletableFuture<String> readString(String url) {
        return readString(url, null, headers, max302);
    }

    @Tool(name = "菜鸟无忧链接读取", value = {"当你需要获取网页内容时，使用此工具，可以获取url链接下的内容"})
    public CompletableFuture<UrlReadToolExecutionResultMessage> read(
            @P(value = "URL", required = false) @Name("urlString") String urlString,
            @ToolMemoryId ToolExecutionRequest request) {
        CompletableFuture<String> future = readString(urlString);
        return future.thenApply(text -> {
            if (!StringUtils.hasText(text)) {
                text = "无结果";
            }
            return new UrlReadToolExecutionResultMessage(request, text, urlString);
        });
    }

    public static class ProxyVO {
        private final String host;
        private final Integer port;

        public ProxyVO(String host, Integer port) {
            this.host = host;
            this.port = port;
        }

        public Integer getPort() {
            return port;
        }

        public String getHost() {
            return host;
        }

        public String toAddressString() {
            if (host != null && port != null) {
                return host + ":" + port;
            } else if (host != null) {
                return host;
            } else {
                return "";
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof ProxyVO)) {
                return false;
            }
            ProxyVO proxyVO = (ProxyVO) o;
            return Objects.equals(host, proxyVO.host) && Objects.equals(port, proxyVO.port);
        }

        @Override
        public int hashCode() {
            return Objects.hash(host, port);
        }
    }

}
