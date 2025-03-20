package com.github.aiassistant.util;

import com.github.aiassistant.platform.SpringParameterNameDiscoverer;

import java.io.IOException;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

public class ParameterNamesUtil {
    private static final Map<Class<?>, Map<Member, String[]>> PARAMETER_NAMES_CACHE = Collections.synchronizedMap(new WeakHashMap<>());
    private static final String[] EMPTY = {};

    public static String[] getParameterNames(Method method) {
        if (SpringParameterNameDiscoverer.isSupport()) {
            return SpringParameterNameDiscoverer.getParameterNames(method);
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
            String[] parameterNames = memberMap.get(method);
            if (parameterNames == null) {
                throw new IllegalStateException("bad method!. object=" + method.getDeclaringClass() + ",method=" + method);
            }
            return parameterNames;
        }
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
        } catch (ClassNotFoundException | IOException | IllegalClassFormatException e) {
            return Collections.emptyMap();
        }
    }
}
