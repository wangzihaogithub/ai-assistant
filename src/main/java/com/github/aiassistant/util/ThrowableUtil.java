package com.github.aiassistant.util;

public class ThrowableUtil {

    public static <E extends Throwable> void sneakyThrows(Throwable t) throws E {
        throw (E) t;
    }
}
