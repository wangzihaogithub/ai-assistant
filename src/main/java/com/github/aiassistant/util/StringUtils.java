package com.github.aiassistant.util;

import java.sql.Timestamp;
import java.util.*;

public class StringUtils {

    public static String substring(String string, int limit, boolean notnull) {
        if (string == null || string.isEmpty()) {
            return notnull ? "" : string;
        }
        return string.length() > limit ? string.substring(0, limit) : string;
    }

    public static List<String> splitString(String string, String split) {
        if (hasText(string)) {
            return Arrays.asList(string.split(split));
        } else {
            return Collections.emptyList();
        }
    }

    public static boolean hasText(String str) {
        if (str != null && !str.isEmpty()) {
            int strLen = str.length();
            for (int i = 0; i < strLen; i++) {
                if (!Character.isWhitespace(str.charAt(i))) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 是否是正整数
     *
     * @param str 字符串
     * @return 是否是正整数
     */
    public static boolean isPositiveNumeric(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        int sz = str.length();
        for (int i = 0; i < sz; i++) {
            char c = str.charAt(i);
            if (!Character.isDigit(c)) {
                return false;
            }
            if (c == '-') {
                return false;
            }
        }
        return true;
    }

    private static Integer[] parseIntegerNumbers(String str) {
        if (str == null) {
            return new Integer[0];
        }
        List<Integer> result = new ArrayList<>();
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c >= '0' && c <= '9') {
                builder.append(c);
            } else if (builder.length() > 0) {
                try {
                    result.add(Integer.valueOf(builder.toString()));
                } catch (Exception e) {
                    return new Integer[0];
                }
                builder.setLength(0);
            }
        }
        if (builder.length() > 0) {
            try {
                result.add(Integer.valueOf(builder.toString()));
            } catch (Exception e) {
                return new Integer[0];
            }
        }
        return result.toArray(new Integer[0]);
    }

    public static Timestamp parseDate(String noHasZoneAnyDateString) {
        if (noHasZoneAnyDateString == null || noHasZoneAnyDateString.isEmpty()) {
            return null;
        }
        int shotTimestampLength = 10;
        int longTimestampLength = 13;
        if (noHasZoneAnyDateString.length() == shotTimestampLength || noHasZoneAnyDateString.length() == longTimestampLength) {
            if (isPositiveNumeric(noHasZoneAnyDateString)) {
                long timestamp = Long.parseLong(noHasZoneAnyDateString);
                if (noHasZoneAnyDateString.length() == shotTimestampLength) {
                    timestamp = timestamp * 1000;
                }
                return new Timestamp(timestamp);
            }
        }
        if ("null".equals(noHasZoneAnyDateString)) {
            return null;
        }
        if ("NULL".equals(noHasZoneAnyDateString)) {
            return null;
        }
        Integer[] numbers = parseIntegerNumbers(noHasZoneAnyDateString);
        if (numbers.length == 0) {
            return null;
        } else {
            if (numbers[0] > 2999 || numbers[0] < 1900) {
                return null;
            }
            if (numbers.length >= 2) {
                if (numbers[1] > 12 || numbers[1] <= 0) {
                    return null;
                }
            }
            if (numbers.length >= 3) {
                if (numbers[2] > 31 || numbers[2] <= 0) {
                    return null;
                }
            }
            if (numbers.length >= 4) {
                if (numbers[3] > 24 || numbers[3] < 0) {
                    return null;
                }
            }
            if (numbers.length >= 5) {
                if (numbers[4] >= 60 || numbers[4] < 0) {
                    return null;
                }
            }
            if (numbers.length >= 6) {
                if (numbers[5] >= 60 || numbers[5] < 0) {
                    return null;
                }
            }
            try {
                Calendar calendar = Calendar.getInstance();
                calendar.set(Calendar.MONTH, 0);
                calendar.set(Calendar.DAY_OF_MONTH, 1);
                calendar.set(Calendar.HOUR_OF_DAY, 0);
                calendar.set(Calendar.MINUTE, 0);
                calendar.set(Calendar.SECOND, 0);
                calendar.set(Calendar.MILLISECOND, 0);
                if (numbers.length == 1) {
                    calendar.set(Calendar.YEAR, numbers[0]);
                } else if (numbers.length == 2) {
                    calendar.set(Calendar.YEAR, numbers[0]);
                    if (noHasZoneAnyDateString.contains("Q") &&
                            (noHasZoneAnyDateString.contains("Q1") || noHasZoneAnyDateString.contains("Q2") || noHasZoneAnyDateString.contains("Q3") || noHasZoneAnyDateString.contains("Q4"))) {
                        calendar.set(Calendar.MONTH, ((numbers[1] - 1) * 3));
                    } else {
                        calendar.set(Calendar.MONTH, numbers[1] - 1);
                    }
                } else if (numbers.length == 3) {
                    calendar.set(Calendar.YEAR, numbers[0]);
                    calendar.set(Calendar.MONTH, numbers[1] - 1);
                    calendar.set(Calendar.DAY_OF_MONTH, numbers[2]);
                } else if (numbers.length == 4) {
                    calendar.set(Calendar.YEAR, numbers[0]);
                    calendar.set(Calendar.MONTH, numbers[1] - 1);
                    calendar.set(Calendar.DAY_OF_MONTH, numbers[2]);
                    calendar.set(Calendar.HOUR_OF_DAY, numbers[3]);
                } else if (numbers.length == 5) {
                    calendar.set(Calendar.YEAR, numbers[0]);
                    calendar.set(Calendar.MONTH, numbers[1] - 1);
                    calendar.set(Calendar.DAY_OF_MONTH, numbers[2]);
                    calendar.set(Calendar.HOUR_OF_DAY, numbers[3]);
                    calendar.set(Calendar.MINUTE, numbers[4]);
                } else {
                    calendar.set(Calendar.YEAR, numbers[0]);
                    calendar.set(Calendar.MONTH, numbers[1] - 1);
                    calendar.set(Calendar.DAY_OF_MONTH, numbers[2]);
                    calendar.set(Calendar.HOUR_OF_DAY, numbers[3]);
                    calendar.set(Calendar.MINUTE, numbers[4]);
                    calendar.set(Calendar.SECOND, numbers[5]);
                }
                return new Timestamp(calendar.getTimeInMillis());
            } catch (Exception e) {
                return null;
            }
        }
    }
}
