package com.teo.ttasks.util;

import android.content.res.Resources;
import android.databinding.BindingAdapter;
import android.support.annotation.DrawableRes;
import android.widget.ImageView;
import android.widget.TextView;

import com.teo.ttasks.R;

import java.util.Date;

public class BindingAdapters {

    @BindingAdapter({"dueDate"})
    public static void bindDueDate(TextView view, Date date) {
        view.setText(date != null ? DateUtils.formatDate(view.getContext(), date) : null);
    }

    @BindingAdapter({"dueTime"})
    public static void bindDueTime(TextView view, Date date) {
        view.setText(date != null ? DateUtils.formatTime(view.getContext(), date) : view.getResources().getString(R.string.due_time_all_day));
    }

    @BindingAdapter({"srcCompat"})
    public static void bindSrcCompat(ImageView view, @DrawableRes int resId) {
        view.setImageResource(resId);
    }

    @BindingAdapter({"completed"})
    public static void bindCompleted(TextView view, Date date) {
        if (date != null) {
            Resources res = view.getResources();
            view.setText(String.format(res.getString(R.string.completed_on), DateUtils.formatDate(view.getContext(), date)));
        } else {
            view.setText(R.string.in_progress);
        }
    }
}
