package com.teo.ttasks.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_LOCALE_CHANGED
import android.content.IntentFilter
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.format.FormatStyle
import java.util.*

class DateUtils private constructor() {
    companion object {

        var formatterTime = getTimeFormatter()
            private set

        var formatterDate = getDateFormatter()
            private set

        /**
         * Has the pattern yyyyMMdd, eg. 20191230
         */
        val formatterDay = DateTimeFormatter.ofPattern("yyyyMMdd")!!

        val formatterDayName = DateTimeFormatter.ofPattern("EEE")!!

        val formatterDayNumber = DateTimeFormatter.ofPattern("d")!!

        val formatterMonth = DateTimeFormatter.ofPattern("MMMM")!!

        val formatterYearMonth = DateTimeFormatter.ofPattern("yyyMM")!!

        val formatterMonthYear = DateTimeFormatter.ofPattern("MMMM, yyyy")!!

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
    }
}
