package com.github.aiassistant.platform;

import com.github.aiassistant.util.HttpClient;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class ApacheHttpClient implements HttpClient {
    private static final Map<String, AtomicInteger> threadNumberMap = new ConcurrentHashMap<>();
    private final ThreadFactory threadFactory;
    private final int maxRedirects;
    volatile CloseableHttpAsyncClient client;
    private boolean ignoreHttpsValidation;
    private Integer connectTimeout;
    private Integer readTimeout;
    private Proxy proxy;

    public ApacheHttpClient(String namePrefix, int maxRedirects) {
        AtomicInteger incr = threadNumberMap.computeIfAbsent(namePrefix, e -> new AtomicInteger());
        this.maxRedirects = maxRedirects;
        this.threadFactory = r -> {
            int i = incr.incrementAndGet();
            Thread t = new Thread(r, namePrefix + i);
            t.setDaemon(true);
            return t;
        };
    }

    private CloseableHttpAsyncClient client() {
        if (client == null) {
            synchronized (this) {
                if (client == null) {
                    ApacheHttpClientBuilder.ClientBuilder clientBuilder = ApacheHttpClientBuilder.newClientBuilder(threadFactory, ignoreHttpsValidation, connectTimeout, readTimeout, proxy, maxRedirects);
                    client = clientBuilder.builder.build();
                    client.start();
                }
            }
        }
        return client;
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
        HttpGet request = new HttpGet();
        request.setURI(uri);
        return new HttpRequest() {

            @Override
            public void setHeader(String key, String value) {
                request.setHeader(key, value);
            }

            @Override
            public CompletableFuture<HttpResponse> connect() {
                CompletableFuture<HttpResponse> future = new CompletableFuture<>();
                client().execute(request, new FutureCallback<org.apache.http.HttpResponse>() {
                    @Override
                    public void completed(org.apache.http.HttpResponse result) {
                        future.complete(new HttpResponse() {
                            @Override
                            public InputStream getInputStream() throws IOException {
                                HttpEntity entity = result.getEntity();
                                return entity == null ? new ByteArrayInputStream(new byte[0]) : entity.getContent();
                            }

                            @Override
                            public String getHeader(String name) {
                                Header[] headers = result.getHeaders(name);
                                return headers == null ? null : Arrays.stream(headers).map(NameValuePair::getValue).collect(Collectors.joining(","));
                            }
                        });
                    }

                    @Override
                    public void failed(Exception ex) {
                        future.completeExceptionally(ex);
                    }

                    @Override
                    public void cancelled() {
                        future.cancel(false);
                    }
                });
                return future;
            }
        };
    }

    @Override
    public void close() throws IOException {
        CloseableHttpAsyncClient client = this.client;
        if (client != null) {
            client.close();
        }
    }

    @Override
    public void setProxy(Proxy proxy) {
        this.proxy = proxy;
    }

    @Override
    public boolean isUseProxy() {
        InetSocketAddress proxyAddress = HttpClient.parseAddress(proxy);
        return proxyAddress != null;
    }

    @Override
    public void ignoreHttpsValidation() {
        this.ignoreHttpsValidation = true;
    }

    @Override
    public void setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    @Override
    public void setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
    }
}