package com.github.aiassistant.platform;

public class PlatformDependentUtil {
    private static final boolean SUPPORT_JSOUP;
    private static final boolean SUPPORT_SPRING_WEB_ASYNC_REST_TEMPLATE;
    private static final boolean SUPPORT_APACHE_HTTP_CLIENT;

    static {
        boolean supportsJSOUP;
        try {
            Class.forName("org.jsoup.Jsoup");
            supportsJSOUP = true;
        } catch (Throwable e) {
            supportsJSOUP = false;
        }
        SUPPORT_JSOUP = supportsJSOUP;

        boolean supportSpringWebAsyncRestTemplate;
        try {
            Class.forName("org.springframework.http.client.HttpComponentsAsyncClientHttpRequestFactory");
            supportSpringWebAsyncRestTemplate = true;
        } catch (Throwable e) {
            supportSpringWebAsyncRestTemplate = false;
        }
        SUPPORT_SPRING_WEB_ASYNC_REST_TEMPLATE = supportSpringWebAsyncRestTemplate;

        boolean supportApacheHttpClient;
        try {
            Class.forName("org.apache.http.impl.nio.client.HttpAsyncClientBuilder");
            supportApacheHttpClient = true;
        } catch (Throwable e) {
            supportApacheHttpClient = false;
        }
        SUPPORT_APACHE_HTTP_CLIENT = supportApacheHttpClient;
    }

    public static boolean isSupportJsoup() {
        return SUPPORT_JSOUP;
    }

    public static boolean isSupportSpringWebAsyncRestTemplate() {
        return SUPPORT_SPRING_WEB_ASYNC_REST_TEMPLATE;
    }

    public static boolean isSupportApacheHttpClient() {
        return SUPPORT_APACHE_HTTP_CLIENT;
    }
}
