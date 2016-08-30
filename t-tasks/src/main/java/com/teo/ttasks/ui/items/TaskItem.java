package com.teo.ttasks.ui.items;

import android.databinding.DataBindingUtil;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.graphics.drawable.VectorDrawableCompat;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;

import com.mikepenz.fastadapter.items.AbstractItem;
import com.teo.ttasks.R;
import com.teo.ttasks.data.model.TTask;
import com.teo.ttasks.databinding.ItemTaskBinding;

import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

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

    @Getter private final String taskId;
    @Getter private final String title;
    @Getter private final String notes;
    @Getter private final Date dueDate;
    @Getter private final Date completed;
    @Getter private final Date reminder;

    /** Flag indicating that this task item should combine with the previous task item in the list */
    private boolean combined;

    public TaskItem(TTask tTask) {
        title = tTask.getTitle();
        notes = tTask.getNotes();
        dueDate = tTask.getDue();
        completed = tTask.getCompleted();
        reminder = tTask.getReminder();
        taskId = tTask.getId();
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
    public void bindView(ViewHolder viewHolder, List payloads) {
        super.bindView(viewHolder, payloads);

        final ItemTaskBinding binding = viewHolder.binding;

        int px = 0;
        RecyclerView.LayoutParams layoutParams = ((RecyclerView.LayoutParams) viewHolder.itemView.getLayoutParams());
        if (combined)
            px = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10, ViewHolder.displayMetrics);
        layoutParams.setMargins(0, -px, 0, 0);
        viewHolder.itemView.setLayoutParams(layoutParams);

        // Title
        binding.taskTitle.setText(title);

        // Task description
        if (notes != null) {
            binding.taskDescription.setText(notes);
            binding.taskDescription.setVisibility(VISIBLE);
        } else {
            binding.taskDescription.setVisibility(GONE);
        }

        // Due date / Completed date
        if (completed != null && !combined) {
            // Mon
            simpleDateFormat.applyLocalizedPattern("EEE");
            binding.dateDayName.setText(simpleDateFormat.format(completed));
            // 1
            simpleDateFormat.applyLocalizedPattern("d");
            binding.dateDayNumber.setText(simpleDateFormat.format(completed));
            // 12:00PM
            binding.dateLayout.setVisibility(VISIBLE);
        } else if (dueDate != null && !combined) {
            // Mon
            simpleDateFormat.applyLocalizedPattern("EEE");
            binding.dateDayName.setText(simpleDateFormat.format(dueDate));
            // 1
            simpleDateFormat.applyLocalizedPattern("d");
            binding.dateDayNumber.setText(simpleDateFormat.format(dueDate));
            // 12:00PM
            binding.dateLayout.setVisibility(VISIBLE);
        } else {
            binding.dateDayNumber.setText(null);
            binding.dateDayName.setText(null);
            if (!combined)
                binding.dateLayout.setVisibility(GONE);
        }

        // Reminder
        if (reminder != null) {
            Timber.d("reminder is not null");
            simpleDateFormat.applyLocalizedPattern("hh:mma");
            binding.reminder.setText(simpleDateFormat.format(reminder));
            binding.reminder.setVisibility(VISIBLE);
        } else {
            binding.reminder.setVisibility(GONE);
        }
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

        static DisplayMetrics displayMetrics;

        public ItemTaskBinding binding;

        public ViewHolder(View view) {
            super(view);
            binding = DataBindingUtil.bind(view);
            Drawable reminderIcon = VectorDrawableCompat.create(view.getResources(), R.drawable.ic_alarm_18dp, view.getContext().getTheme());
            binding.reminder.setCompoundDrawablesWithIntrinsicBounds(reminderIcon, null, null, null);
            displayMetrics = view.getContext().getResources().getDisplayMetrics();
        }
    }
}
