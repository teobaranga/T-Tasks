package com.teo.ttasks.ui.items;

import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.graphics.drawable.VectorDrawableCompat;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;
import android.widget.TextView;

import com.mikepenz.fastadapter.items.AbstractItem;
import com.teo.ttasks.R;
import com.teo.ttasks.data.model.TTask;

import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import lombok.Getter;
import lombok.experimental.Accessors;
import timber.log.Timber;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

@Accessors()
public class TaskItem extends AbstractItem<TaskItem, TaskItem.ViewHolder> implements Comparable<TaskItem> {

    private static final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("EEE", Locale.getDefault());
    private static final SimpleDateFormat fmt = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());

    /**
     * Comparator that sorts {@link TaskItem}s by their completion date in descending order
     */
    public static Comparator<TaskItem> completionDateComparator = (lhs, rhs) -> {

        boolean sameDay = fmt.format(lhs.completed).equals(fmt.format(rhs.completed));

        int returnCode;

        // Check the completed date
        if (lhs.completed == null && rhs.completed == null) {
            returnCode = 0;
        } else if (lhs.completed == null) {
            returnCode = 1;
        } else if (rhs.completed == null) {
            returnCode = -1;
        } else {
            returnCode = rhs.completed.compareTo(lhs.completed);
        }

        if (sameDay) {
            if (returnCode == 0 || returnCode == -1)
                rhs.combined = true;
            else lhs.combined = true;
        }

        return returnCode;
    };

    private static DisplayMetrics sDisplayMetrics;

    @Getter public String taskId;
    @Getter private String title;
    @Getter private String notes;
    @Getter private Date dueDate;
    @Getter private Date completed;
    @Getter private Date reminder;

    /** Flag indicating that this task item should combine with the previous task item in the list */
    private boolean combined;

    public TaskItem(TTask task) {
        title = task.getTitle();
        notes = task.getNotes();
        dueDate = task.getDue();
        completed = task.getCompleted();
        reminder = task.getReminder();
        taskId = task.getId();
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

        int px = 0;
        RecyclerView.LayoutParams layoutParams = ((RecyclerView.LayoutParams) viewHolder.itemView.getLayoutParams());
        if (combined)
            px = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10, sDisplayMetrics);
        layoutParams.setMargins(0, -px, 0, 0);
        viewHolder.itemView.setLayoutParams(layoutParams);

        // Task description
        if (notes == null) {
            viewHolder.taskDescription.setVisibility(GONE);
        } else {
            viewHolder.taskDescription.setText(notes);
            viewHolder.taskDescription.setVisibility(VISIBLE);
        }

        if (completed != null && !combined) {
            // Mon
            simpleDateFormat.applyLocalizedPattern("EEE");
            viewHolder.dateDayName.setText(simpleDateFormat.format(completed));
            // 1
            simpleDateFormat.applyLocalizedPattern("d");
            viewHolder.dateDayNumber.setText(simpleDateFormat.format(completed));
            // 12:00PM
            viewHolder.layoutDate.setVisibility(VISIBLE);
        } else if (dueDate != null && !combined) {
            // Mon
            simpleDateFormat.applyLocalizedPattern("EEE");
            viewHolder.dateDayName.setText(simpleDateFormat.format(dueDate));
            // 1
            simpleDateFormat.applyLocalizedPattern("d");
            viewHolder.dateDayNumber.setText(simpleDateFormat.format(dueDate));
            // 12:00PM
            viewHolder.layoutDate.setVisibility(VISIBLE);
        } else {
            viewHolder.dateDayNumber.setText(null);
            viewHolder.dateDayName.setText(null);
            if (!combined)
                viewHolder.layoutDate.setVisibility(GONE);
        }

        if (reminder != null) {
            Timber.d("reminder is not null");
            simpleDateFormat.applyLocalizedPattern("hh:mma");
            viewHolder.reminderTime.setText(simpleDateFormat.format(reminder));
            viewHolder.reminderTime.setVisibility(VISIBLE);
        } else {
            viewHolder.reminderTime.setVisibility(GONE);
        }

        // Set item views based on the data model
        viewHolder.taskTitle.setText(title);
    }

    /**
     * Compare by due date and return results in ascending order. <br>
     * Orders with missing due dates are considered high priority and they stay at the top
     */
    @Override
    public int compareTo(@NonNull TaskItem another) {
        if (dueDate != null) {
            // Compare non-null due dates, most recent ones at the top
            if (another.dueDate != null)
                return another.dueDate.compareTo(dueDate);
            // This task comes after the other task
            return 1;
        } else if (another.dueDate != null) {
            // This task comes before the other task
            return -1;
        }
        // Both tasks have missing due dates, they are considered equal
        return 0;
    }

    //The viewHolder used for this item. This viewHolder is always reused by the RecyclerView so scrolling is blazing fast
    public static class ViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.layout_task) public View taskLayout;
        @BindView(R.id.task_title) TextView taskTitle;
        @BindView(R.id.task_description) TextView taskDescription;
        @BindView(R.id.layout_date) View layoutDate;
        @BindView(R.id.date_day_number) TextView dateDayNumber;
        @BindView(R.id.date_day_name) TextView dateDayName;
        @BindView(R.id.task_reminder) TextView reminderTime;

        public ViewHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
            Drawable reminderIcon = VectorDrawableCompat.create(view.getResources(), R.drawable.ic_alarm_18dp, view.getContext().getTheme());
            reminderTime.setCompoundDrawablesWithIntrinsicBounds(reminderIcon, null, null, null);
            sDisplayMetrics = view.getContext().getResources().getDisplayMetrics();
        }
    }
}
