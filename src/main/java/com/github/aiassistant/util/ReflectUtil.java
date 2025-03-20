package com.github.aiassistant.util;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ReflectUtil {
    private static final int ALLOWED_MODES = MethodHandles.Lookup.PRIVATE | MethodHandles.Lookup.PROTECTED
            | MethodHandles.Lookup.PACKAGE | MethodHandles.Lookup.PUBLIC;
    private static Constructor<MethodHandles.Lookup> java8LookupConstructor;
    private static Method privateLookupInMethod;

    static {
        //先查询jdk9 开始提供的java.lang.invoke.MethodHandles.privateLookupIn方法,
        //如果没有说明是jdk8的版本.(不考虑jdk8以下版本)
        try {
            //noinspection JavaReflectionMemberAccess
            privateLookupInMethod = MethodHandles.class.getMethod("privateLookupIn", Class.class, MethodHandles.Lookup.class);
        } catch (NoSuchMethodException ignore) {
            //ignore
        }

        //jdk8
        //这种方式其实也适用于jdk9及以上的版本,但是上面优先,可以避免 jdk9 反射警告
        if (privateLookupInMethod == null) {
            try {
                java8LookupConstructor = MethodHandles.Lookup.class.getDeclaredConstructor(Class.class, int.class);
                java8LookupConstructor.setAccessible(true);
            } catch (NoSuchMethodException e) {
                //可能是jdk8 以下版本
                throw new IllegalStateException(
                        "There is neither 'privateLookupIn(Class, Lookup)' nor 'Lookup(Class, int)' method in java.lang.invoke.MethodHandles.", e);
            }
        }
    }

    public static <T> T invokeMethodHandle(boolean special,Object obj, Method method, Object[] args) throws Throwable {
        final Class<?> declaringClass = method.getDeclaringClass();
        final MethodHandles.Lookup lookup = lookup(declaringClass);
        MethodHandle handle = special? lookup.unreflectSpecial(method, declaringClass)
                : lookup.unreflect(method);
        if (null != obj) {
            handle = handle.bindTo(obj);
        }
        return (T) handle.invokeWithArguments(args);

    }

    private static MethodHandles.Lookup lookup(Class<?> callerClass) throws InvocationTargetException, InstantiationException, IllegalAccessException {
        //使用反射,因为当前jdk可能不是java9或以上版本
        if (privateLookupInMethod != null) {
            return (MethodHandles.Lookup) privateLookupInMethod.invoke(MethodHandles.class, callerClass, MethodHandles.lookup());
        }
        //jdk 8
        return java8LookupConstructor.newInstance(callerClass, ALLOWED_MODES);
    }

}
