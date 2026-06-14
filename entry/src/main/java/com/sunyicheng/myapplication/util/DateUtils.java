package com.sunyicheng.myapplication.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 日期格式化工具类
 */
public class DateUtils {

    private static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";
    private static final String DATE_PATTERN = "yyyy-MM-dd";
    private static final String DATE_PATTERN_CN = "yyyy年MM月dd日";

    /**
     * 获取当前日期时间字符串 yyyy-MM-dd HH:mm:ss
     */
    public static String getCurrentDateTime() {
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_TIME_PATTERN, Locale.getDefault());
        return sdf.format(new Date());
    }

    /**
     * 获取当前日期字符串 yyyy-MM-dd
     */
    public static String getCurrentDate() {
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_PATTERN, Locale.getDefault());
        return sdf.format(new Date());
    }

    /**
     * 格式化日期为中文格式 yyyy年MM月dd日
     */
    public static String formatDateCn(String dateStr) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat(DATE_PATTERN, Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat(DATE_PATTERN_CN, Locale.getDefault());
            Date date = inputFormat.parse(dateStr);
            return outputFormat.format(date);
        } catch (Exception e) {
            return dateStr;
        }
    }

    /**
     * 格式化日期时间为中文格式
     */
    public static String formatDateTimeCn(String dateTimeStr) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat(DATE_TIME_PATTERN, Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy年MM月dd日 HH:mm", Locale.getDefault());
            Date date = inputFormat.parse(dateTimeStr);
            return outputFormat.format(date);
        } catch (Exception e) {
            return dateTimeStr;
        }
    }
}
