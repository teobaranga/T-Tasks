package com.teo.ttasks.util;

import android.content.Context;
import android.text.format.DateUtils;

import java.util.Date;

public class DateUtil {

    private static final int dateFlags = DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_ABBREV_MONTH | DateUtils.FORMAT_SHOW_WEEKDAY |
            DateUtils.FORMAT_ABBREV_WEEKDAY | DateUtils.FORMAT_SHOW_YEAR;

    private static final int timeFlags = DateUtils.FORMAT_SHOW_TIME;

    public static String formatDate(Context context, Date date) {
        return DateUtils.formatDateTime(context, date.getTime(), dateFlags);
    }

    public static String formatTime(Context context, Date date) {
        return DateUtils.formatDateTime(context, date.getTime(), timeFlags);
    }
}
