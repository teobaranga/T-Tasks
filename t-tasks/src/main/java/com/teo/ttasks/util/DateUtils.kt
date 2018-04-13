package com.teo.ttasks.util

import android.content.Context
import java.text.DateFormatSymbols
import java.text.SimpleDateFormat
import java.util.*

class DateUtils private constructor() : android.text.format.DateUtils() {
    companion object {
        /** The date pattern used to parse and format dates for Google Tasks */
        const val DATE_PATTERN: String = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"

        val utcDateFormat = SimpleDateFormat(DateUtils.DATE_PATTERN, Locale.getDefault())

        private const val dateFlags = FORMAT_SHOW_DATE or FORMAT_ABBREV_MONTH or FORMAT_SHOW_WEEKDAY or FORMAT_ABBREV_WEEKDAY or FORMAT_SHOW_YEAR
        private const val timeFlags = FORMAT_SHOW_TIME

        private const val SAME_DAY_PATTERN = "yyyyMMdd"
        val sdfDay = SimpleDateFormat(SAME_DAY_PATTERN, Locale.getDefault())

        private const val SAME_MONTH_PATTERN = "yyyMM"
        val sdfMonth = SimpleDateFormat(SAME_MONTH_PATTERN, Locale.getDefault())

        private const val DAY_NAME_PATTERN = "EEE"
        val sdfDayName = SimpleDateFormat(DAY_NAME_PATTERN, Locale.getDefault())

        private const val DAY_NUMBER_PATTERN = "d"
        val sdfDayNumber = SimpleDateFormat(DAY_NUMBER_PATTERN, Locale.getDefault())

        init {
            val utc = TimeZone.getTimeZone("UTC")
            sdfDay.timeZone = utc
            sdfMonth.timeZone = utc
            sdfDayName.timeZone = utc
            sdfDayNumber.timeZone = utc
            utcDateFormat.timeZone = utc
        }

        fun formatDate(context: Context, date: Date): String {
            val f = Formatter(StringBuilder(50), Locale.getDefault())
            val timeMillis = date.time
            return formatDateRange(context, f, timeMillis, timeMillis, dateFlags, "UTC").toString()
        }

        fun formatTime(context: Context, date: Date): String {
            return formatDateTime(context, date.time, timeFlags)
        }

        fun getMonthAndYear(date: Date): String {
            val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
            calendar.time = date
            return String.format(Locale.getDefault(), "%s, %d",
                    DateFormatSymbols.getInstance().months[calendar.get(Calendar.MONTH)], calendar.get(Calendar.YEAR))
        }
    }
}
