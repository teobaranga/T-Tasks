package com.teo.ttasks.util;

import android.databinding.BindingAdapter;
import android.support.annotation.DrawableRes;
import android.widget.ImageView;
import android.widget.TextView;

import com.teo.ttasks.R;

import java.util.Date;

public class BindingAdapters {

    @BindingAdapter({"dueDate"})
    public static void bindDueDate(TextView view, Date date) {
        view.setText(date != null ? DateUtil.formatDate(view.getContext(), date) : view.getResources().getString(R.string.due_date_missing));
    }

    @BindingAdapter({"dueTime"})
    public static void bindDueTime(TextView view, Date date) {
        view.setText(date != null ? DateUtil.formatTime(view.getContext(), date) : view.getResources().getString(R.string.due_time_all_day));
    }

    @BindingAdapter({"srcCompat"})
    public static void bindSrcCompat(ImageView view, @DrawableRes int resId) {
        view.setImageResource(resId);
    }
}
