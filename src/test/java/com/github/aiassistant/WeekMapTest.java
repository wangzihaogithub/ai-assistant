package com.github.aiassistant;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

public class WeekMapTest {
    private static final Map<String, Map<float[], String>> GLOABL_CACHE_VECTOR_MAP = new ConcurrentHashMap<>();

    private static Map<float[], String> getModelCache(String modelName) {
        return GLOABL_CACHE_VECTOR_MAP.computeIfAbsent(modelName,
                k -> Collections.synchronizedMap(new WeakHashMap<>(256)));
    }

    public static void main(String[] args) {
        Map<float[], String> modelCache = getModelCache("k1");
        testSet();
        testGet();
        System.gc();
        System.out.println("gc");
        testGet();
        System.out.println("modelCache = " + modelCache);
    }

    private static void testSet() {
        Map<float[], String> modelCache = getModelCache("k1");
        modelCache.put(new float[]{1f, 23f, 43f}, "1");
        modelCache.put(new float[]{3f, 25f, 43f}, "2");
        System.out.println("testSet = " + modelCache.size());
    }

    private static void testGet() {
        Map<float[], String> modelCache = getModelCache("k1");
        modelCache.forEach((key, value) -> {
            System.out.println(Arrays.toString(key) + " = " + value);
        });
        System.out.println("testGet = " + modelCache.size());
    }
}
