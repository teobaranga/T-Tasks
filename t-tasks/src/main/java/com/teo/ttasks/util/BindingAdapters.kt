package com.teo.ttasks.util

import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.databinding.BindingAdapter
import com.teo.ttasks.R
import java.util.*

object BindingAdapters {

    @BindingAdapter("dueDate")
    @JvmStatic
    fun bindDueDate(view: TextView, date: Date?) {
        view.text = if (date != null) DateUtils.formatDate(view.context, date) else null
    }

    @BindingAdapter("dueTime")
    @JvmStatic
    fun bindDueTime(view: TextView, date: Date?) {
        view.text = if (date != null) DateUtils.formatTime(
            view.context,
            date
        ) else view.resources.getString(R.string.due_time_all_day)
    }

    @BindingAdapter("reminder")
    @JvmStatic
    fun bindReminder(view: TextView, date: Date?) {
        view.text = if (date != null) DateUtils.formatTime(view.context, date) else null
    }

    @BindingAdapter("srcCompat")
    @JvmStatic
    fun bindSrcCompat(view: ImageView, @DrawableRes resId: Int) {
        view.setImageResource(resId)
    }

    @BindingAdapter("completed")
    @JvmStatic
    fun bindCompleted(view: TextView, date: Date?) {
        if (date != null) {
            val res = view.resources
            view.text = String.format(res.getString(R.string.completed_on), DateUtils.formatDate(view.context, date))
        } else {
            view.setText(R.string.in_progress)
        }
    }
}
