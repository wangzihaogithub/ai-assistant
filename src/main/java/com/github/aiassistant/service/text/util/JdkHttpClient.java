package com.github.aiassistant.service.text.util;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.security.SecureRandom;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JdkHttpClient implements HttpClient {
    private Integer connectTimeout;
    private Integer readTimeout;
    private Boolean ignoreHttpsValidation;
    private Proxy proxy;

    public static URL expand(String template, Map<String, ?> variables) throws MalformedURLException {
        Pattern pattern = Pattern.compile("\\{([^{}]+)\\}");
        Matcher matcher = pattern.matcher(template);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String variableName = matcher.group(1);
            Object replacement = variables == null ? null : variables.get(variableName);
            if (replacement == null) {
                replacement = "";
            }
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement.toString()));
        }
        matcher.appendTail(sb);
        return new URL(sb.toString());
    }

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
        URL url = expand(uriTemplate, uriVariables == null ? Collections.emptyMap() : uriVariables);
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