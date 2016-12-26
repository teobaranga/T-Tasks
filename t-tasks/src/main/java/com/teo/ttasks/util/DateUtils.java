package com.teo.ttasks.util;

import android.content.Context;

import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Formatter;
import java.util.Locale;
import java.util.TimeZone;

public class DateUtils extends android.text.format.DateUtils {

    private static final int dateFlags = FORMAT_SHOW_DATE | FORMAT_ABBREV_MONTH | FORMAT_SHOW_WEEKDAY | FORMAT_ABBREV_WEEKDAY | FORMAT_SHOW_YEAR;
    private static final int timeFlags = FORMAT_SHOW_TIME;

    private static final String SAME_DAY_PATTERN = "yyyyMMdd";
    public static final SimpleDateFormat sdfDay = new SimpleDateFormat(SAME_DAY_PATTERN, Locale.getDefault());

    private static final String SAME_MONTH_PATTERN = "yyyMM";
    public static final SimpleDateFormat sdfMonth = new SimpleDateFormat(SAME_MONTH_PATTERN, Locale.getDefault());

    private static final String DAY_NAME_PATTERN = "EEE";
    public static final SimpleDateFormat sdfDayName = new SimpleDateFormat(DAY_NAME_PATTERN, Locale.getDefault());

    private static final String DAY_NUMBER_PATTERN = "d";
    public static final SimpleDateFormat sdfDayNumber = new SimpleDateFormat(DAY_NUMBER_PATTERN, Locale.getDefault());

    static {
        final TimeZone utc = TimeZone.getTimeZone("UTC");
        sdfDay.setTimeZone(utc);
        sdfMonth.setTimeZone(utc);
        sdfDayName.setTimeZone(utc);
        sdfDayNumber.setTimeZone(utc);
    }

    private DateUtils() { }

    public static String formatDate(Context context, Date date) {
        Formatter f = new Formatter(new StringBuilder(50), Locale.getDefault());
        final long timeMillis = date.getTime();
        return formatDateRange(context, f, timeMillis, timeMillis, dateFlags, "UTC").toString();
    }

    public static String formatTime(Context context, Date date) {
        return formatDateTime(context, date.getTime(), timeFlags);
    }

    public static String getMonthAndYear(Date date) {
        final Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calendar.setTime(date);
        return String.format(Locale.getDefault(), "%s, %d",
                DateFormatSymbols.getInstance().getMonths()[calendar.get(Calendar.MONTH)], calendar.get(Calendar.YEAR));
    }
}
