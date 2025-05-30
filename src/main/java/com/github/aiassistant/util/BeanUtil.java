package com.github.aiassistant.util;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public class BeanUtil {

    public static <T> T newInstance(Class<T> clazz) {
        try {
            Constructor<T> declaredConstructor = clazz.getDeclaredConstructor();
            declaredConstructor.setAccessible(true);
            return declaredConstructor.newInstance();
        } catch (Throwable e) {
            ThrowableUtil.sneakyThrows(e);
            return null;
        }
    }

    /**
     * 将source对象的属性值复制到target对象中
     *
     * @param source 源对象
     * @param target 目标对象
     */
    public static void copyProperties(Object source, Object target) {
        if (source == null || target == null) {
            throw new NullPointerException("Source and target must not be null");
        }

        try {
            // 获取源对象和目标对象的Bean信息
            BeanInfo sourceBeanInfo = Introspector.getBeanInfo(source.getClass());
            BeanInfo targetBeanInfo = Introspector.getBeanInfo(target.getClass());

            // 获取所有属性描述符
            PropertyDescriptor[] sourcePropertyDescriptors = sourceBeanInfo.getPropertyDescriptors();
            PropertyDescriptor[] targetPropertyDescriptors = targetBeanInfo.getPropertyDescriptors();

            // 遍历源对象的属性
            for (PropertyDescriptor sourcePropertyDescriptor : sourcePropertyDescriptors) {
                String propertyName = sourcePropertyDescriptor.getName();
                // 忽略class属性
                if ("class".equals(propertyName)) {
                    continue;
                }
                if (targetPropertyDescriptors == null || targetPropertyDescriptors.length == 0) {
                    continue;
                }
                // 查找目标对象中对应的属性描述符
                Optional<PropertyDescriptor> propertyDescriptor = Arrays.stream(targetPropertyDescriptors).filter(e -> e.getName().equals(propertyName))
                        .findFirst();

                Method targetSetter = propertyDescriptor.map(PropertyDescriptor::getWriteMethod)
                        .orElse(null);
                // 获取源对象的getter方法和目标对象的setter方法
                Method sourceGetter = sourcePropertyDescriptor.getReadMethod();
                if (sourceGetter != null && targetSetter != null) {
                    if (!targetSetter.isAccessible()) {
                        targetSetter.setAccessible(true);
                    }
                    // 调用getter方法获取源对象的属性值
                    Object value = sourceGetter.invoke(source);
                    // 这里应该添加类型检查，以确保sourceValue可以安全地传递给targetSetter
                    // 但为了简化示例，我们省略了这一步
                    // 调用setter方法设置目标对象的属性值
                    Object cast = cast(value, propertyDescriptor.orElse(null));
                    targetSetter.invoke(target, cast);
                }
            }
        } catch (Exception e) {
            ThrowableUtil.sneakyThrows(e);
        }
    }

    public static <T> T toBean(Object source, Class<T> beanClass) {
        if (source instanceof Map) {
            return toBean((Map<String, Object>) source, beanClass);
        }
        T instance;
        try {
            instance = beanClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            ThrowableUtil.sneakyThrows(e);
            return null;
        }
        copyProperties(source, instance);
        return instance;
    }

    /**
     * 将Map中的数据转换为JavaBean对象的属性
     *
     * @param map       包含属性名和值的Map
     * @param beanClass 要转换成的JavaBean类
     * @param <T>       bean
     * @return 转换后的JavaBean对象
     */
    public static <T> T toBean(Map<String, Object> map, Class<T> beanClass) {
        try {
            // 创建JavaBean的实例
            T bean = beanClass.newInstance();
            // 获取JavaBean的BeanInfo
            BeanInfo beanInfo = Introspector.getBeanInfo(beanClass);

            // 获取JavaBean的所有属性描述符
            PropertyDescriptor[] propertyDescriptors = beanInfo.getPropertyDescriptors();

            // 遍历Map中的每个键值对
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                String propertyName = entry.getKey();
                Object propertyValue = entry.getValue();

                // 查找对应的属性描述符
                for (PropertyDescriptor propertyDescriptor : propertyDescriptors) {
                    if (propertyDescriptor.getName().equals(propertyName)) {
                        // 获取setter方法
                        Method setter = propertyDescriptor.getWriteMethod();

                        // 如果setter方法存在，则调用它设置属性值
                        if (setter != null) {
                            if (!setter.isAccessible()) {
                                setter.setAccessible(true);
                            }
                            Object cast = cast(propertyValue, propertyDescriptor);
                            setter.invoke(bean, cast);
                        }
                        break; // 找到匹配的属性后退出内层循环
                    }
                }
            }
            return bean;
        } catch (Exception e) {
            ThrowableUtil.sneakyThrows(e);
            return null;
        }
    }

    private static Object cast(Object value, PropertyDescriptor propertyDescriptor) {
        Class<?> propertyType = propertyDescriptor.getPropertyType();
        if (propertyType != null) {
            return TypeUtil.cast(value, propertyType);
        } else {
            return value;
        }
    }

    private static boolean isJavaPackage(Class clazz) {
        Package aPackage = clazz.getPackage();
        return aPackage == null || aPackage.getName().startsWith("java.");
    }

    private static void flatMap(Map<String, Object> map, String prefix, Object bean) {
        if (bean == null || "".equals(bean)) {
            map.put(prefix, " ");
            return;
        }
        if (!(bean instanceof Map) && isJavaPackage(bean.getClass())) {
            map.put(prefix, bean);
        } else {
            Map<String, Object> beanMap = bean instanceof Map ? (Map<String, Object>) bean : new BeanMap(bean);
            for (Map.Entry<String, Object> entry : beanMap.entrySet()) {
                String p = (prefix.isEmpty() ? "" : prefix + ".") + entry.getKey();
                Object value = entry.getValue();
                flatMap(map, p, value);
            }
        }
    }

    public static Map<String, Object> toMap(Object bean) {
        Map<String, Object> map = new LinkedHashMap<>();
        Map<String, Object> beanMap = bean instanceof Map ? (Map<String, Object>) bean : new BeanMap(bean);
        for (Map.Entry<String, Object> entry : beanMap.entrySet()) {
            flatMap(map, entry.getKey(), entry.getValue());
        }
        return map;
    }
}
