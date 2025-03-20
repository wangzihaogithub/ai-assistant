package com.github.aiassistant.util;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class CollUtil {
    public static <T, R> List<R> map(Iterable<T> collection, Function<? super T, ? extends R> func, boolean ignoreNull) {
        final List<R> fieldValueList = new ArrayList<>();
        if (null == collection) {
            return fieldValueList;
        }

        R value;
        for (T t : collection) {
            if (null == t && ignoreNull) {
                continue;
            }
            value = func.apply(t);
            if (null == value && ignoreNull) {
                continue;
            }
            fieldValueList.add(value);
        }
        return fieldValueList;
    }
}
