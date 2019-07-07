package com.teo.ttasks.util

import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.databinding.BindingAdapter
import com.teo.ttasks.R
import org.threeten.bp.ZonedDateTime

object BindingAdapters {

    @BindingAdapter("dueDate")
    @JvmStatic
    fun bindDueDate(view: TextView, date: ZonedDateTime?) {
        view.text = DateUtils.formatterDate.format(date)
    }

    @BindingAdapter("dueTime")
    @JvmStatic
    fun bindDueTime(view: TextView, date: ZonedDateTime?) {
        view.text = when {
            date != null -> DateUtils.formatterTime.format(date)
            else -> view.resources.getString(R.string.due_time_all_day)
        }
    }

    @BindingAdapter("reminder")
    @JvmStatic
    fun bindReminderDate(view: TextView, date: ZonedDateTime?) {
        view.text = when {
            date != null -> DateUtils.formatterTime.format(date)
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
                String.format(res.getString(R.string.completed_on), DateUtils.formatterDate.format(date))
        } else {
            view.setText(R.string.in_progress)
        }
    }
}
