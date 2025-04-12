package com.github.aiassistant.util;

import com.github.aiassistant.platform.PlatformDependentUtil;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.security.SecureRandom;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class JdkHttpClient implements HttpClient {
    private Integer connectTimeout;
    private Integer readTimeout;
    private Boolean ignoreHttpsValidation;
    private Proxy proxy;

    @Override
    public void setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    @Override
    public void setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
    }

    @Override
    public HttpRequest request(String uriTemplate, Map<String, ?> uriVariables) throws MalformedURLException {
        Map<String, ?> notnullUriVariables = uriVariables == null ? Collections.emptyMap() : uriVariables;
        URI uri;
        try {
            uri = PlatformDependentUtil.uriTemplateExpand(uriTemplate, notnullUriVariables);
        } catch (URISyntaxException e) {
            throw new MalformedURLException(uriTemplate + " is not a valid URL");
        }
        URL url = uri.toURL();
        URLConnection connection;
        try {
            if (proxy != null) {
                // JDK 8u111版本后，目标页面为HTTPS协议，启用proxy用户密码鉴权
                System.setProperty("jdk.http.auth.tunneling.disabledSchemes", "");
                connection = url.openConnection(proxy);
            } else {
                connection = url.openConnection();
            }
        } catch (IOException e) {
            if (e instanceof MalformedURLException) {
                throw (MalformedURLException) e;
            } else {
                throw new MalformedURLException(uriTemplate + " is not a valid URL");
            }
        }
        if (connectTimeout != null) {
            connection.setConnectTimeout(connectTimeout);
        }
        if (readTimeout != null) {
            connection.setReadTimeout(readTimeout);
        }
        if (ignoreHttpsValidation != null && ignoreHttpsValidation) {
            if (connection instanceof HttpsURLConnection) {
                try {
                    SSLContext ctx = SSLContext.getInstance("TLS");
                    ctx.init((KeyManager[]) null, new TrustManager[]{disableValidationTrustManager}, new SecureRandom());
                    ((HttpsURLConnection) connection).setSSLSocketFactory(ctx.getSocketFactory());
                    ((HttpsURLConnection) connection).setHostnameVerifier(trustAllHostnames);
                } catch (Exception ignored) {
                }
            }
        }
        return new HttpRequest() {
            @Override
            public void setHeader(String key, String value) {
                connection.setRequestProperty(key, value);
            }

            @Override
            public CompletableFuture<HttpResponse> connect() {
                InputStream inputStream;
                try {
                    connection.connect();
                    inputStream = connection.getInputStream();
                } catch (Exception e) {
                    CompletableFuture<HttpResponse> future = new CompletableFuture<>();
                    future.completeExceptionally(e);
                    return future;
                }
                return CompletableFuture.completedFuture(new HttpResponse() {
                    @Override
                    public InputStream getInputStream() {
                        return inputStream;
                    }

                    @Override
                    public String getHeader(String name) {
                        return connection.getHeaderField(name);
                    }
                });
            }
        };
    }

    @Override
    public void close() throws IOException {

    }

    @Override
    public void setProxy(Proxy proxy) {
        this.proxy = proxy;
    }

    @Override
    public boolean isUseProxy() {
        return proxy != null;
    }

    @Override
    public void ignoreHttpsValidation() {
        this.ignoreHttpsValidation = true;
    }
}