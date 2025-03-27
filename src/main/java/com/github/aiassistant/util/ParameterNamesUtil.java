package com.github.aiassistant.util;

import com.github.aiassistant.platform.SpringParameterNameDiscoverer;

import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Predicate;

public class ParameterNamesUtil {
    private static final Map<Class<?>, Map<Member, String[]>> PARAMETER_NAMES_CACHE = Collections.synchronizedMap(new WeakHashMap<>());
    private static final String[] EMPTY = {};

    public static String[] getParameterNames(Method method, Predicate<Parameter> nameMissPredicate) {
        if (nameMissPredicate == null) {
            nameMissPredicate = parameter -> false;
        }
        String[] parameterNames;
        if (SpringParameterNameDiscoverer.isSupport()) {
            parameterNames = SpringParameterNameDiscoverer.getParameterNames(method);
        } else {
            Class<?> declaringClass = method.getDeclaringClass();
            if (declaringClass.isInterface()) {
                return EMPTY;
            }
            Map<Member, String[]> memberMap = PARAMETER_NAMES_CACHE.get(declaringClass);
            if (memberMap == null) {
                memberMap = readParameterNameMap(declaringClass);
                PARAMETER_NAMES_CACHE.put(declaringClass, memberMap);
            }
            parameterNames = memberMap.get(method);
        }
        Parameter[] parameters = method.getParameters();
        if (parameterNames == null) {
            parameterNames = new String[parameters.length];
            for (int i = 0; i < parameters.length; i++) {
                Parameter parameter = parameters[i];
                Name annotation = parameter.getAnnotation(Name.class);
                String name;
                if (annotation != null) {
                    name = annotation.value();
                } else if (nameMissPredicate.test(parameter)) {
                    throw new IllegalStateException("Name for argument of method [" + method + "] not specified, and parameter name information not available via reflection. Ensure that the compiler uses the '-parameters' flag. or use annotation @com.github.aiassistant.util.Name");
                } else {
                    name = parameter.getName();
                }
                parameterNames[i] = name;
            }
        } else {
            for (int i = 0; i < parameters.length; i++) {
                Parameter parameter = parameters[i];
                Name annotation = parameter.getAnnotation(Name.class);
                if (annotation != null) {
                    parameterNames[i] = annotation.value();
                }
            }
        }
        return parameterNames;
    }

    public static Map<Member, String[]> readParameterNameMap(Class<?> clazz) {
        try {
            Java8ClassFile javaClassFile = new Java8ClassFile(clazz);
            Map<Member, String[]> result = new HashMap<>(6);
            for (Java8ClassFile.Member member : javaClassFile.getMethods()) {
                try {
                    Member javaMember = member.toJavaMember();
                    String[] parameterNames = member.getParameterNames();
                    result.put(javaMember, parameterNames);
                } catch (Exception e) {
                    throw e;
                }
            }
            return result;
        } catch (Throwable e) {
            return Collections.emptyMap();
        }
    }
}
