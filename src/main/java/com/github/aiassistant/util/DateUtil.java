package com.github.aiassistant.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class DateUtil {

    public static Date offsetDay(Date date, int offsetDays) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.DAY_OF_MONTH, offsetDays);
        return calendar.getTime();
    }

    public static Date offsetWeek(Date date, int offsetWeeks) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);

        // 设置周计算规则（根据需求调整）
        calendar.setFirstDayOfWeek(Calendar.MONDAY);
        calendar.setMinimalDaysInFirstWeek(4);

        calendar.add(Calendar.WEEK_OF_YEAR, offsetWeeks);
        return calendar.getTime();
    }

    public static Date offsetMonth(Date date, int offsetMonths) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.MONTH, offsetMonths);
        return calendar.getTime();
    }

    public static String dateformat(Date date, String pattern) {
        return new SimpleDateFormat(pattern).format(date);
    }

}
