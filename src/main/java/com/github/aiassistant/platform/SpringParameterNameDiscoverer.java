package com.github.aiassistant.platform;

import com.github.aiassistant.util.ThrowableUtil;

import java.lang.reflect.Method;

public class SpringParameterNameDiscoverer {
    private static final String SPRING_FRAMEWORK
            = "org.springframework.core.LocalVariableTableParameterNameDiscoverer";
    private static final Method GET_PARAMETER_NAMES;
    private static final Object INSTANCE;

    static {
        Object instance;
        Method method;
        try {
            Class<?> aClass = Class.forName(SPRING_FRAMEWORK);
            method = aClass.getDeclaredMethod("getParameterNames", Method.class);
            method.setAccessible(true);
            instance = aClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            method = null;
            instance = null;
        }
        GET_PARAMETER_NAMES = method;
        INSTANCE = instance;
    }

    public static boolean isSupport() {
        return GET_PARAMETER_NAMES != null && INSTANCE != null;
    }

    public static String[] getParameterNames(Method method) {
        try {
            return (String[]) GET_PARAMETER_NAMES.invoke(INSTANCE, method);
        } catch (Exception e) {
            ThrowableUtil.sneakyThrows(e);
            return null;
        }
    }
}
