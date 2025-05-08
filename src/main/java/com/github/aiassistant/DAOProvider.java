package com.github.aiassistant;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

public abstract class DAOProvider {
    private static final Map<Class<?>, List<Function<Object, Object>>> PROXY_LIST_MAP = new ConcurrentHashMap<>();

    public static <T> void addProxy(Class<T> mapperClass, Function<T, T> proxy) {
        PROXY_LIST_MAP.computeIfAbsent(mapperClass, k -> new CopyOnWriteArrayList<>())
                .add((Function<Object, Object>) proxy);
    }

    public <T> T getMapper(Class<T> mapperClass) {
        Collection<Function<Object, Object>> proxyList = PROXY_LIST_MAP.get(mapperClass);
        T instance = instance(mapperClass);
        if (proxyList != null) {
            for (Function<Object, Object> classBiFunction : proxyList) {
                instance = (T) classBiFunction.apply(instance);
            }
        }
        return instance;
    }

    protected abstract <T> T instance(Class<T> mapperClass);
}
