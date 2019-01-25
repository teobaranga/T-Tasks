package com.teo.ttasks.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_LOCALE_CHANGED
import android.content.IntentFilter
import android.text.format.DateUtils.*
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.format.FormatStyle
import java.text.SimpleDateFormat
import java.util.*

class DateUtils private constructor() {
    companion object {
        /** The date pattern used to parse and format dates for Google Tasks */
        const val DATE_PATTERN: String = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"

        val utcDateFormat = SimpleDateFormat(DateUtils.DATE_PATTERN, Locale.getDefault())

        var formatterTime = getTimeFormatter()
            private set

        var formatterDate = getDateFormatter()
            private set

        val formatterDay = DateTimeFormatter.ofPattern("yyyyMMdd")!!

        val formatterDayName = DateTimeFormatter.ofPattern("EEE")!!

        val formatterDayNumber = DateTimeFormatter.ofPattern("d")!!

        val formatterMonth = DateTimeFormatter.ofPattern("yyyMM")!!

        val formatterMonthYear = DateTimeFormatter.ofPattern("MMMM, yyyy")!!

        private const val dateFlags =
            FORMAT_SHOW_DATE or FORMAT_ABBREV_MONTH or FORMAT_SHOW_WEEKDAY or FORMAT_ABBREV_WEEKDAY or FORMAT_SHOW_YEAR
        private const val timeFlags = FORMAT_SHOW_TIME

        init {
            val utc = TimeZone.getTimeZone("UTC")
            utcDateFormat.timeZone = utc
        }

        fun init(context: Context) {
            context.registerReceiver(object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    formatterTime = getTimeFormatter()
                    formatterDate = getDateFormatter()
                }
            }, IntentFilter(ACTION_LOCALE_CHANGED))
        }

        private fun getTimeFormatter() =
            DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(Locale.getDefault())!!

        private fun getDateFormatter() =
            DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(Locale.getDefault())!!

        fun formatDate(context: Context, date: Date): String {
            val f = Formatter(StringBuilder(50), Locale.getDefault())
            val timeMillis = date.time
            return formatDateRange(context, f, timeMillis, timeMillis, dateFlags, "UTC").toString()
        }

        fun formatTime(context: Context, date: Date): String {
            return formatDateTime(context, date.time, timeFlags)
        }
    }
}
