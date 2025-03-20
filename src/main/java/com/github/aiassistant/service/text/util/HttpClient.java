package com.github.aiassistant.service.text.util;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.X509TrustManager;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface HttpClient {
    X509TrustManager disableValidationTrustManager = new X509TrustManager() {

        @Override
        public void checkClientTrusted(X509Certificate[] x509Certificates, String s)
                throws CertificateException {
            if (x509Certificates == null || x509Certificates.length == 0 || s == null) {
                throw new CertificateException("certificate empty");
            }
        }

        @Override
        public void checkServerTrusted(X509Certificate[] x509Certificates, String s)
                throws CertificateException {
            if (x509Certificates == null || x509Certificates.length == 0 || s == null) {
                throw new CertificateException("certificate empty");
            }
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }

    };
    HostnameVerifier trustAllHostnames = (hostname, session) -> hostname != null || session != null;

    HttpRequest request(String uriTemplate, Map<String, ?> uriVariables) throws MalformedURLException;

    void setProxy(Proxy proxy);

    boolean isUseProxy();

    void ignoreHttpsValidation();

    void setConnectTimeout(int connectTimeout);

    void setReadTimeout(int readTimeout);

    interface HttpRequest {
        void setHeader(String key, String value);

        CompletableFuture<HttpResponse> connect();
    }

    interface HttpResponse {
        InputStream getInputStream();

        String getHeader(String name);
    }
}



