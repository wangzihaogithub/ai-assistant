package com.github.aiassistant.platform;

public class PlatformDependentUtil {
    private static final boolean SUPPORT_JSOUP;

    static {
        boolean supportsJSOUP;
        try {
            Class.forName("org.jsoup.Jsoup");
            supportsJSOUP = true;
        } catch (Throwable e) {
            supportsJSOUP = false;
        }
        SUPPORT_JSOUP = supportsJSOUP;
    }

    public static boolean isSupportJsoup() {
        return SUPPORT_JSOUP;
    }
}
