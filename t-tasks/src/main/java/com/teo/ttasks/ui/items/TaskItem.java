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
import com.mikepenz.fastadapter.utils.ViewHolderFactory;
import com.teo.ttasks.R;
import com.teo.ttasks.data.model.TTask;
import com.teo.ttasks.databinding.ItemTaskBinding;
import com.teo.ttasks.util.DateUtils;

import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import lombok.Getter;
import lombok.experimental.Accessors;
import timber.log.Timber;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

@Accessors()
public class TaskItem extends AbstractItem<TaskItem, TaskItem.ViewHolder> implements Comparable<TaskItem> {

    private static final ViewHolderFactory<? extends ViewHolder> FACTORY = new ItemFactory();

    private static final String DAY_PATTERN = "EEE";
    private static final String DAY_NUMBER_PATTERN = "d";

    private static final SimpleDateFormat simpleDateFormat = new SimpleDateFormat(DAY_PATTERN, Locale.getDefault());
    private static final SimpleDateFormat fmt = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());

    /**
     * Comparator that sorts {@link TaskItem}s by their completion date in descending order.
     */
    public static Comparator<TaskItem> completionDateComparator = (lhs, rhs) -> {

        final boolean sameDay = fmt.format(lhs.completed).equals(fmt.format(rhs.completed));

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

    public static Comparator<TaskItem> alphabeticalComparator = (lhs, rhs) -> {
        // Titles should never be null or empty but just in case
        final boolean noTitleLeft = lhs.title == null || lhs.title.isEmpty();
        final boolean noTitleRight = rhs.title == null || rhs.title.isEmpty();
        if (noTitleLeft && noTitleRight)
            return 0;
        if (noTitleLeft)
            return -1;
        if (noTitleRight)
            return 1;
        return lhs.title.compareToIgnoreCase(rhs.title);
    };

    /**
     * Comparator that sorts {@link TaskItem}s by their due dates in ascending order. Default comparator.
     */
    private static Comparator<TaskItem> dueDateComparator = (lhs, rhs) -> {

        final boolean sameDay = lhs.dueDate != null && rhs.dueDate != null && fmt.format(lhs.dueDate).equals(fmt.format(rhs.dueDate));

        int returnCode;

        if (lhs.dueDate != null) {
            // Compare non-null due dates, most recent ones at the top
            if (rhs.dueDate != null)
                returnCode = lhs.dueDate.compareTo(rhs.dueDate);
            else
                // This task comes after the other task
                returnCode = 1;
        } else if (rhs.dueDate != null) {
            // This task comes before the other task
            returnCode = -1;
        } else {
            // Both tasks have missing due dates, they are considered equal
            returnCode = 0;
        }

        if (sameDay) {
            if (returnCode == 0 || returnCode == -1)
                lhs.combined = true;
            else rhs.combined = true;
        }

        return returnCode;
    };

    static {
        simpleDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

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
    public ViewHolderFactory<? extends ViewHolder> getFactory() {
        return FACTORY;
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
            px = viewHolder.topMargin;
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
            simpleDateFormat.applyLocalizedPattern(DAY_PATTERN);
            binding.dateDayName.setText(simpleDateFormat.format(completed));
            // 1
            simpleDateFormat.applyLocalizedPattern(DAY_NUMBER_PATTERN);
            binding.dateDayNumber.setText(simpleDateFormat.format(completed));
            // 12:00PM
            binding.dateLayout.setVisibility(VISIBLE);
        } else if (dueDate != null && !combined) {
            // Mon
            simpleDateFormat.applyLocalizedPattern(DAY_PATTERN);
            binding.dateDayName.setText(simpleDateFormat.format(dueDate));
            // 1
            simpleDateFormat.applyLocalizedPattern(DAY_NUMBER_PATTERN);
            binding.dateDayNumber.setText(simpleDateFormat.format(dueDate));
            // 12:00PM
            binding.dateLayout.setVisibility(VISIBLE);
        } else {
            binding.dateDayNumber.setText(null);
            binding.dateDayName.setText(null);
            binding.dateLayout.setVisibility(!combined ? GONE : VISIBLE);
        }

        // Reminder
        if (reminder != null) {
            Timber.d("reminder is not null");
            binding.reminder.setText(DateUtils.formatTime(binding.getRoot().getContext(), reminder));
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
        return dueDateComparator.compare(this, another);
    }

    private static class ItemFactory implements ViewHolderFactory<ViewHolder> {
        public ViewHolder create(View v) {
            return new ViewHolder(v);
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        final int topMargin;

        public ItemTaskBinding binding;

        ViewHolder(View view) {
            super(view);
            binding = DataBindingUtil.bind(view);
            Drawable reminderIcon = VectorDrawableCompat.create(view.getResources(), R.drawable.ic_alarm_18dp, view.getContext().getTheme());
            binding.reminder.setCompoundDrawablesWithIntrinsicBounds(reminderIcon, null, null, null);
            DisplayMetrics displayMetrics = view.getContext().getResources().getDisplayMetrics();
            topMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10, displayMetrics);
        }
    }
}
