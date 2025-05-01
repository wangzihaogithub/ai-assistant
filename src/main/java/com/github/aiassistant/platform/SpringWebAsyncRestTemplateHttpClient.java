package com.github.aiassistant.platform;

import com.github.aiassistant.util.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.AsyncClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsAsyncClientHttpRequestFactory;
import org.springframework.web.client.AsyncRestTemplate;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.SocketAddress;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class SpringWebAsyncRestTemplateHttpClient implements HttpClient {
    private static final Map<String, AtomicInteger> threadNumberMap = new ConcurrentHashMap<>();
    private final ThreadFactory threadFactory;
    private final int maxRedirects;
    private volatile AsyncRestTemplate template;
    private boolean ignoreHttpsValidation;
    private Integer connectTimeout;
    private Integer readTimeout;
    private Proxy proxy;

    public SpringWebAsyncRestTemplateHttpClient(String namePrefix, int maxRedirects) {
        AtomicInteger incr = threadNumberMap.computeIfAbsent(namePrefix, e -> new AtomicInteger());
        this.maxRedirects = maxRedirects;
        this.threadFactory = r -> {
            int i = incr.incrementAndGet();
            Thread t = new Thread(r, namePrefix + i);
            t.setDaemon(true);
            return t;
        };
    }

    @Override
    public HttpRequest request(String uriTemplate, Map<String, ?> uriVariables) throws MalformedURLException {
        return new HttpRequest() {
            private final HttpHeaders headers = new HttpHeaders();

            @Override
            public void setHeader(String key, String value) {
                headers.set(key, value);
            }

            @Override
            public CompletableFuture<HttpResponse> connect() {
                return template().exchange(uriTemplate, HttpMethod.GET, new HttpEntity<>(headers), byte[].class, uriVariables == null ? Collections.emptyMap() : uriVariables)
                        .completable()
                        .thenApply(responseEntity -> new HttpResponse() {
                            @Override
                            public InputStream getInputStream() {
                                if (responseEntity == null || responseEntity.getBody() == null) {
                                    return new ByteArrayInputStream(new byte[0]);
                                }
                                return new ByteArrayInputStream(responseEntity.getBody());
                            }

                            @Override
                            public String getHeader(String name) {
                                Collection<String> list = responseEntity.getHeaders().get(name);
                                return list == null ? null : String.join(",", list);
                            }
                        });
            }
        };
    }

    @Override
    public void close() throws IOException {
        if (template == null) {
            return;
        }
        AsyncClientHttpRequestFactory asyncRequestFactory = template.getAsyncRequestFactory();
        if (asyncRequestFactory instanceof DisposableBean) {
            try {
                ((DisposableBean) asyncRequestFactory).destroy();
            } catch (Exception e) {
                if (e instanceof IOException) {
                    throw (IOException) e;
                } else {
                    throw new IOException("close exception", e);
                }
            }
        }
    }

    @Override
    public void setProxy(Proxy proxy) {
        this.proxy = proxy;
    }

    @Override
    public boolean isUseProxy() {
        InetSocketAddress proxyAddress = getProxyAddress();
        return proxyAddress != null;
    }

    public AsyncRestTemplate template() {
        if (template == null) {
            synchronized (this) {
                if (template == null) {
                    AsyncClientHttpRequestFactory factory;
                    factory = clientApache();
                    template = new AsyncRestTemplate(factory);
                }
            }
        }
        return template;
    }

    private InetSocketAddress getProxyAddress() {
        if (proxy != null && proxy.type() == Proxy.Type.HTTP) {
            SocketAddress address = proxy.address();
            if (address instanceof InetSocketAddress) {
                return (InetSocketAddress) address;
            }
        }
        return null;
    }

//    private AsyncClientHttpRequestFactory clientNetty() {
//        Netty4ClientHttpRequestFactory factory = new Netty4ClientHttpRequestFactory(
//                new NioEventLoopGroup(Runtime.getRuntime().availableProcessors() * 2, threadFactory));
//        if (readTimeout != null) {
//            factory.setReadTimeout(readTimeout);
//        }
//        if (connectTimeout != null) {
//            factory.setConnectTimeout(connectTimeout);
//        }
//        if (ignoreHttpsValidation) {
//            try {
//                SslContext sslContext = SslContextBuilder.forClient()
//                        .trustManager(InsecureTrustManagerFactory.INSTANCE) // 忽略所有证书验证
//                        .build();
//                factory.setSslContext(sslContext);
//            } catch (Exception ignored) {
//            }
//        }
//        return factory;
//    }

    private AsyncClientHttpRequestFactory clientApache() {
        ApacheHttpClientBuilder.ClientBuilder clientBuilder = ApacheHttpClientBuilder.newClientBuilder(threadFactory, ignoreHttpsValidation, connectTimeout, readTimeout, proxy, maxRedirects);
        HttpComponentsAsyncClientHttpRequestFactory factory = new HttpComponentsAsyncClientHttpRequestFactory(clientBuilder.builder.build()) {
            @Override
            protected RequestConfig createRequestConfig(Object client) {
                return clientBuilder.requestConfig;
            }
        };
        return factory;

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