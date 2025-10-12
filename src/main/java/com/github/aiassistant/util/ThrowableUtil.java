package com.github.aiassistant.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.CompletionException;

public class ThrowableUtil {

    public static <E extends Throwable> void sneakyThrows(Throwable t) throws E {
        throw (E) t;
    }

    public static Throwable getCause(Throwable throwable) {
        Throwable rootError = throwable;
        while (rootError instanceof CompletionException || rootError instanceof InvocationTargetException) {
            rootError = rootError.getCause();
        }
        return rootError;
    }

    public static String stackTraceToString(Throwable cause) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream pout = new PrintStream(out);
        cause.printStackTrace(pout);
        pout.flush();
        try {
            return out.toString();
        } finally {
            try {
                out.close();
            } catch (IOException ignore) {
                // ignore as should never happen
            }
        }
    }
}
