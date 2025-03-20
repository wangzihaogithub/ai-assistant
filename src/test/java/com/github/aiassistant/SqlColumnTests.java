package com.github.aiassistant;

import com.github.aiassistant.entity.AiChatAbort;
import com.github.aiassistant.entity.AiMemoryMessage;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

class SqlColumnTests {

    private static final Pattern HUMP_PATTERN = Pattern.compile("[A-Z]");

    public static void main(String[] args) throws IntrospectionException {
        Class clazz = AiMemoryMessage.class;
        String column = column(clazz);
        String values = values(clazz, "item");

        System.out.println(column);
        System.out.println(values);
    }

    private static String column(Class clazz) throws IntrospectionException {
        List<String> list = new ArrayList<>();
        BeanInfo sourceBeanInfo = Introspector.getBeanInfo(clazz);
        for (PropertyDescriptor descriptor : sourceBeanInfo.getPropertyDescriptors()) {
            String name = descriptor.getName();
            if (name.equals("class")) {
                continue;
            }
            list.add(humpToLine(name));
        }
        return "(`" + String.join("`,`", list) + "`)";
    }

    private static String values(Class clazz, String item) throws IntrospectionException {
        List<String> list = new ArrayList<>();
        BeanInfo sourceBeanInfo = Introspector.getBeanInfo(clazz);
        for (PropertyDescriptor descriptor : sourceBeanInfo.getPropertyDescriptors()) {
            String name = descriptor.getName();
            if (name.equals("class")) {
                continue;
            }
            list.add(name);
        }
        String prifx = item.isEmpty() ? "#{" : "#{" + item + ".";
        return prifx + list.stream().collect(Collectors.joining("},  " + prifx)) + "}";
    }

    /**
     * 驼峰转下划线,效率比上面高
     *
     * @param str 字符串
     * @return 下划线
     */
    public static String humpToLine(String str) {
        Matcher matcher = HUMP_PATTERN.matcher(str);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(sb, "_" + matcher.group(0).toLowerCase());
        }
        matcher.appendTail(sb);
        return sb.toString();
    }
}
