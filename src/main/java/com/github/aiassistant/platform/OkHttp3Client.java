package com.github.aiassistant.platform;

import com.github.aiassistant.util.HttpClient;
import okhttp3.*;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class OkHttp3Client implements HttpClient {
    private final String namePrefix;
    private Duration connectTimeout = Duration.ofSeconds(30);
    private Duration readTimeout = Duration.ofSeconds(30);

    private OkHttpClient client;
    private boolean close = false;
    private Proxy proxy;
    private Boolean ignoreHttpsValidation;

    public OkHttp3Client(String namePrefix) {
        this.namePrefix = namePrefix;
    }

    private OkHttpClient getClient() {
        if (close) {
            throw new IllegalStateException("OkHttp3Client is close!");
        }
        if (client == null) {
            synchronized (this) {
                if (client == null) {
                    OkHttpClient.Builder okHttpClientBuilder = new OkHttpClient.Builder()
                            .callTimeout(connectTimeout.plus(readTimeout))
                            .connectTimeout(connectTimeout)
                            .readTimeout(readTimeout)
                            .writeTimeout(readTimeout);
                    if (proxy != null) {
                        okHttpClientBuilder.proxy(proxy);
                    }
                    if (ignoreHttpsValidation != null && ignoreHttpsValidation) {
                        try {
                            SSLContext ctx = SSLContext.getInstance("TLS");
                            ctx.init((KeyManager[]) null, new TrustManager[]{disableValidationTrustManager}, new SecureRandom());
                            okHttpClientBuilder.sslSocketFactory(ctx.getSocketFactory(), disableValidationTrustManager);
                            okHttpClientBuilder.hostnameVerifier(trustAllHostnames);
                        } catch (Exception ignored) {
                        }
                    }
                    okHttpClientBuilder.dispatcher(new Dispatcher(new ThreadPoolExecutor(0, Integer.MAX_VALUE, 60, TimeUnit.SECONDS,
                            new SynchronousQueue<>(), runnable -> {
                        Thread result = new Thread(runnable);
                        result.setName(namePrefix + result.getId());
                        result.setDaemon(true);
                        return result;
                    })));
                    client = okHttpClientBuilder.build();
                }
            }
        }
        return client;
    }

    @Override
    public HttpRequest request(String uriTemplate, Map<String, ?> uriVariables) throws MalformedURLException {
        OkHttpClient client = getClient();
        Map<String, ?> notnullUriVariables = uriVariables == null ? Collections.emptyMap() : uriVariables;
        URI uri;
        try {
            uri = PlatformDependentUtil.uriTemplateExpand(uriTemplate, notnullUriVariables);
        } catch (URISyntaxException e) {
            throw new MalformedURLException(uriTemplate + " is not a valid URL");
        }
        URL url = uri.toURL();
        Request.Builder requestBuilder = new Request.Builder()
                .url(url)
                .get();
        return new HttpRequest() {
            @Override
            public void setHeader(String key, String value) {
                requestBuilder.header(key, value);
            }

            @Override
            public CompletableFuture<HttpResponse> connect() {
                Call call = client.newCall(requestBuilder.build());
                CompletableFuture<HttpResponse> future = new CompletableFuture<HttpResponse>() {
                    @Override
                    public boolean cancel(boolean mayInterruptIfRunning) {
                        if (isCancelled()) {
                            return false;
                        }
                        call.cancel();
                        return super.cancel(mayInterruptIfRunning);
                    }
                };
                call.enqueue(new Callback() {

                    @Override
                    public void onFailure(Call call, IOException e) {
                        future.completeExceptionally(e);
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        future.complete(new HttpResponse() {
                            @Override
                            public InputStream getInputStream() throws IOException {
                                ResponseBody body = response.body();
                                if (body == null) {
                                    return new ByteArrayInputStream(new byte[0]);
                                }
                                return body.byteStream();
                            }

                            @Override
                            public String getHeader(String name) {
                                return response.header(name);
                            }
                        });
                    }
                });
                return future;
            }
        };
    }

    @Override
    public void close() throws IOException {
        if (close) {
            return;
        }
        close = true;
        OkHttpClient client = this.client;
        this.client = null;
        if (client != null) {
            try {
                client.dispatcher().executorService().shutdown();
            } catch (Exception e) {
                // ignore
            }
            try {
                client.connectionPool().evictAll();
            } catch (Exception e) {
                // ignore
            }
            Cache cache = client.cache();
            if (cache != null) {
                try {
                    cache.close();
                } catch (Exception e) {
                    // ignore
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
        return proxy != null;
    }

    @Override
    public void ignoreHttpsValidation() {
        this.ignoreHttpsValidation = true;
    }

    @Override
    public void setConnectTimeout(int connectTimeout) {
        this.connectTimeout = Duration.ofMillis(connectTimeout);
    }

    @Override
    public void setReadTimeout(int readTimeout) {
        this.readTimeout = Duration.ofMillis(readTimeout);
    }
}
