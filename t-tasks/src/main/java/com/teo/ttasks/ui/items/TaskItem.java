package com.teo.ttasks.ui.items;

import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.graphics.drawable.VectorDrawableCompat;
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

import butterknife.BindView;
import butterknife.ButterKnife;
import lombok.Getter;
import lombok.experimental.Accessors;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

@Accessors(prefix = "m")
public class TaskItem extends AbstractItem<TaskItem, TaskItem.ViewHolder> implements Comparable<TaskItem> {

    private static final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("EEE", Locale.getDefault());

    /**
     * Comparator that sorts {@link TaskItem}s by their completion date in descending order
     */
    public static Comparator<TaskItem> completionDateComparator = (lhs, rhs) -> {
        // Check the due date
        if (lhs.mDueDate == null && rhs.mDueDate == null)
            return 0;
        else if (lhs.mDueDate == null)
            return -1;
        else if (rhs.mDueDate == null)
            return 1;

        // Check the completed date
        if (lhs.mCompleted == null && rhs.mCompleted == null)
            return 0;
        if (lhs.mCompleted == null)
            return 1;
        if (rhs.mCompleted == null)
            return -1;

        return rhs.mCompleted.compareTo(lhs.mCompleted);
    };

    @Getter private String mTitle;
    @Getter private String mNotes;
    @Getter private Date mDueDate;
    @Getter private Date mCompleted;
    @Getter private Date mReminderDate;
    @Getter public String mTaskId;

    public TaskItem(@NonNull Task task) {
        mTitle = task.getTitle();
        mNotes = task.getNotes();
        mDueDate = task.getDue();
        mCompleted = task.getCompleted();
        mReminderDate = task.getReminder();
        mTaskId = task.getId();
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
            viewHolder.layoutDate.setVisibility(VISIBLE);
        } else {
            viewHolder.dateDayNumber.setText(null);
            viewHolder.dateDayName.setText(null);
            viewHolder.layoutDate.setVisibility(GONE);
        }

        if (mReminderDate != null) {
            simpleDateFormat.applyLocalizedPattern("hh:mma");
            viewHolder.reminderTime.setText(simpleDateFormat.format(mReminderDate));
            viewHolder.reminderTime.setVisibility(VISIBLE);
        } else {
            viewHolder.reminderTime.setVisibility(GONE);
        }

        // Set item views based on the data model
        viewHolder.taskTitle.setText(mTitle);
    }

    /**
     * Compare by due date and return results in ascending order. <br>
     * Orders with missing due dates are considered high priority and they stay at the top
     */
    @Override
    public int compareTo(@NonNull TaskItem another) {
        if (mDueDate != null) {
            // Compare non-null due dates
            if (another.mDueDate != null)
                return mDueDate.compareTo(another.mDueDate);
            // This task comes after the other task
            return 1;
        } else if (another.mDueDate != null) {
            // This task comes before the other task
            return -1;
        }
        // Both tasks have missing due dates, they are considered equal
        return 0;
    }

    //The viewHolder used for this item. This viewHolder is always reused by the RecyclerView so scrolling is blazing fast
    public static class ViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.layout_task) public View taskLayout;
        @BindView(R.id.task_title) public TextView taskTitle;
        @BindView(R.id.task_description) TextView taskDescription;
        @BindView(R.id.layout_date) View layoutDate;
        @BindView(R.id.date_day_number) TextView dateDayNumber;
        @BindView(R.id.date_day_name) TextView dateDayName;
        @BindView(R.id.task_reminder) TextView reminderTime;

        public ViewHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
            Drawable reminderIcon = VectorDrawableCompat.create(view.getResources(), R.drawable.ic_alarm_18dp, view.getContext().getTheme());
            reminderTime.setCompoundDrawables(reminderIcon, null, null, null);
        }
    }
}
