package com.teo.ttasks.ui.items;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import com.mikepenz.fastadapter.items.AbstractItem;
import com.teo.ttasks.R;
import com.teo.ttasks.data.model.Task;

import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;

import butterknife.Bind;
import butterknife.ButterKnife;
import lombok.Getter;
import lombok.experimental.Accessors;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

@Accessors(prefix = "m")
public class TaskItem extends AbstractItem<TaskItem, TaskItem.ViewHolder> implements Comparable<TaskItem> {

    private static final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("EEE", Locale.getDefault());

    public static Comparator<TaskItem> completionDateComparator = new Comparator<TaskItem>() {
        @Override
        public int compare(TaskItem lhs, TaskItem rhs) {
            if (lhs.mCompleted == null && rhs.mCompleted == null)
                return 0;
            if (lhs.mCompleted == null)
                return 1;
            if (rhs.mCompleted == null)
                return -1;
            return rhs.mCompleted.compareTo(lhs.mCompleted);
        }
    };

    @Getter
    private String mTitle;

    @Getter
    private String mNotes;

    @Getter
    private Date mDueDate;

    @Getter
    private Date mCompleted;

    @Getter
    private Date mReminderDate;

    public TaskItem(@NonNull Task task) {
        mTitle = task.getTitle();
        mNotes = task.getNotes();
        mDueDate = task.getDue();
        mCompleted = task.getCompleted();
        mReminderDate = task.getReminder();
    }

    @Override
    public int getType() {
        return R.id.task_item;
    }

    @Override
    public int getLayoutRes() {
        return R.layout.item_task;
    }

    @Override
    public void bindView(ViewHolder viewHolder) {
        super.bindView(viewHolder);

        // Task description
        if (mNotes == null) {
            viewHolder.taskDescription.setVisibility(GONE);
        } else {
            viewHolder.taskDescription.setText(mNotes);
            viewHolder.taskDescription.setVisibility(VISIBLE);
        }

        if (mDueDate != null) {
            // Mon
            simpleDateFormat.applyLocalizedPattern("EEE");
            viewHolder.dateDayName.setText(simpleDateFormat.format(mDueDate));
            // 1
            simpleDateFormat.applyLocalizedPattern("d");
            viewHolder.dateDayNumber.setText(simpleDateFormat.format(mDueDate));
            // 12:00PM
            if (mReminderDate != null) {
                simpleDateFormat.applyLocalizedPattern("hh:mma");
                viewHolder.reminderTime.setText(simpleDateFormat.format(mReminderDate));
                viewHolder.reminderTime.setVisibility(VISIBLE);
            } else {
                viewHolder.reminderTime.setVisibility(GONE);
            }
        } else {
            viewHolder.dateDayNumber.setText(null);
            viewHolder.dateDayName.setText(null);
        }

        // Set item views based on the data model
        viewHolder.taskTitle.setText(mTitle);
    }

    @Override
    public int compareTo(@NonNull TaskItem another) {
        if (mDueDate != null && another.mDueDate != null)
            return mDueDate.compareTo(another.mDueDate);
        return 0;
    }

    //The viewHolder used for this item. This viewHolder is always reused by the RecyclerView so scrolling is blazing fast
    protected static class ViewHolder extends RecyclerView.ViewHolder {

        @Bind(R.id.task_title) TextView taskTitle;
        @Bind(R.id.task_description) TextView taskDescription;
        @Bind(R.id.date_day_number) TextView dateDayNumber;
        @Bind(R.id.date_day_name) TextView dateDayName;
        @Bind(R.id.task_reminder) TextView reminderTime;

        public ViewHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
        }
    }
}
