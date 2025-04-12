package com.github.aiassistant.platform;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlatformDependentUtil {
    private static final Pattern URI_TEMPLATE_EXPAND_PATTERN = Pattern.compile("\\{([^{}]+)\\}");
    private static final boolean SUPPORT_JSOUP;
    private static final boolean SUPPORT_SPRING_WEB_ASYNC_REST_TEMPLATE;
    private static final boolean SUPPORT_APACHE_HTTP_CLIENT;
    private static final Constructor<?> URI_BUILDER_FACTORY_CONSTRUCTOR;
    private static final Method SPRING_EXPAND_METHOD;

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

        Method expandMethod;
        Constructor<?> defaultUriBuilderFactoryConstructor;
        try {
            Class<?> defaultUriBuilderFactory = Class.forName("org.springframework.web.util.DefaultUriBuilderFactory");
            defaultUriBuilderFactoryConstructor = defaultUriBuilderFactory.getConstructor();
            defaultUriBuilderFactoryConstructor.setAccessible(true);
            expandMethod = defaultUriBuilderFactory.getDeclaredMethod("expand", String.class, Map.class);
            expandMethod.setAccessible(true);
        } catch (Throwable e) {
            defaultUriBuilderFactoryConstructor = null;
            expandMethod = null;
        }
        SPRING_EXPAND_METHOD = expandMethod;
        URI_BUILDER_FACTORY_CONSTRUCTOR = defaultUriBuilderFactoryConstructor;
    }

    public static URI uriTemplateExpand(String baseUriTemplate, Map<String, ?> variables) throws URISyntaxException {
        URI uri = springUriBuilderExpand(baseUriTemplate, variables);
        if (uri == null) {
            Matcher matcher = URI_TEMPLATE_EXPAND_PATTERN.matcher(baseUriTemplate);
            StringBuffer sb = new StringBuffer();
            while (matcher.find()) {
                String variableName = matcher.group(1);
                Object replacement = variables == null ? null : variables.get(variableName);
                if (replacement == null) {
                    replacement = "";
                }
                String replace = replacement.toString().replace(" ", "%20");
                try {
                    replace = URLEncoder.encode(replace, "UTF-8");
                } catch (UnsupportedEncodingException ignored) {
                }
                matcher.appendReplacement(sb, Matcher.quoteReplacement(replace));
            }
            matcher.appendTail(sb);
            uri = new URI(sb.toString());
        }
        return uri;
    }

    public static boolean isSupportSpringUriBuilderExpand() {
        return SPRING_EXPAND_METHOD != null && URI_BUILDER_FACTORY_CONSTRUCTOR != null;
    }

    public static URI springUriBuilderExpand(String baseUriTemplate, Map<String, ?> uriVars) {
        if (isSupportSpringUriBuilderExpand()) {
            try {
                Object instance = URI_BUILDER_FACTORY_CONSTRUCTOR.newInstance();
                Object invoke = SPRING_EXPAND_METHOD.invoke(instance, baseUriTemplate, uriVars);
                if (invoke instanceof URI) {
                    return (URI) invoke;
                } else {
                    return null;
                }
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                return null;
            }
        } else {
            return null;
        }
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
