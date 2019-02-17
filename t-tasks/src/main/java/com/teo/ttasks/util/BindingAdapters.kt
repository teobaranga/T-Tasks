package com.teo.ttasks.util

import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.databinding.BindingAdapter
import com.teo.ttasks.R
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.format.DateTimeFormatter

object BindingAdapters {

    @BindingAdapter("dueDate")
    @JvmStatic
    fun bindDueDate(view: TextView, date: ZonedDateTime?) {
        view.text = date?.format(DateTimeFormatter.ISO_ZONED_DATE_TIME)
    }

    @BindingAdapter("dueTime")
    @JvmStatic
    fun bindDueTime(view: TextView, date: ZonedDateTime?) {
        view.text = when {
            date != null -> date.format(DateTimeFormatter.ISO_LOCAL_TIME)
            else -> view.resources.getString(R.string.due_time_all_day)
        }
    }

    @BindingAdapter("reminder")
    @JvmStatic
    fun bindReminderDate(view: TextView, date: ZonedDateTime?) {
        view.text = when {
            date != null -> date.format(DateTimeFormatter.ISO_LOCAL_TIME)
            else -> null
        }
    }

    @BindingAdapter("srcCompat")
    @JvmStatic
    fun bindSrcCompat(view: ImageView, @DrawableRes resId: Int) {
        view.setImageResource(resId)
    }

    @BindingAdapter("completed")
    @JvmStatic
    fun bindCompletedDate(view: TextView, date: ZonedDateTime?) {
        if (date != null) {
            val res = view.resources
            view.text =
                String.format(res.getString(R.string.completed_on), date.format(DateTimeFormatter.ISO_ZONED_DATE_TIME))
        } else {
            view.setText(R.string.in_progress)
        }
    }
}
