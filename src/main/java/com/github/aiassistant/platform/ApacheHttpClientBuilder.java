package com.github.aiassistant.platform;

import com.github.aiassistant.util.HttpClient;
import org.apache.http.HttpHost;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.security.SecureRandom;
import java.util.concurrent.ThreadFactory;

class ApacheHttpClientBuilder {

    static ClientBuilder newClientBuilder(ThreadFactory threadFactory,
                                          boolean ignoreHttpsValidation,
                                          Integer connectTimeout,
                                          Integer readTimeout,
                                          Proxy proxy) {
        HttpHost proxyHttp = null;
        InetSocketAddress proxyAddress = HttpClient.parseAddress(proxy);
        if (proxyAddress != null) {
            proxyHttp = new HttpHost(proxyAddress.getHostString(), proxyAddress.getPort(), "http");
        }
        RequestConfig.Builder requestConfigBuilder = RequestConfig.custom();
        if (readTimeout != null) {
            requestConfigBuilder.setConnectionRequestTimeout(readTimeout);
            requestConfigBuilder.setSocketTimeout(readTimeout);
        }
        if (connectTimeout != null) {
            requestConfigBuilder.setConnectTimeout(connectTimeout);
        }
        if (proxyHttp != null) {
            requestConfigBuilder.setProxy(proxyHttp);
        }
        requestConfigBuilder.setMaxRedirects(1);
        RequestConfig requestConfig = requestConfigBuilder.build();
        HttpAsyncClientBuilder system = HttpAsyncClientBuilder.create()
                .useSystemProperties()
                .setThreadFactory(threadFactory)
                .setDefaultRequestConfig(requestConfig);
        if (proxyHttp != null) {
            system.setProxy(proxyHttp);
        }
        if (ignoreHttpsValidation) {
            try {
                SSLContext ctx = SSLContext.getInstance("TLS");
                ctx.init((KeyManager[]) null, new TrustManager[]{HttpClient.disableValidationTrustManager}, new SecureRandom());
                system.setSSLContext(ctx);
                system.setSSLHostnameVerifier(HttpClient.trustAllHostnames);
            } catch (Exception ignored) {
            }
        }
        return new ClientBuilder(system, requestConfig);
    }

    public static class ClientBuilder {
        public final HttpAsyncClientBuilder builder;
        public final RequestConfig requestConfig;

        public ClientBuilder(HttpAsyncClientBuilder builder, RequestConfig requestConfig) {
            this.builder = builder;
            this.requestConfig = requestConfig;
        }
    }
}
