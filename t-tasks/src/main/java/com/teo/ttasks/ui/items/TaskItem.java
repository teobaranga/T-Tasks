package com.teo.ttasks.ui.items;

import android.databinding.DataBindingUtil;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.graphics.drawable.VectorDrawableCompat;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;

import com.mikepenz.fastadapter.FastAdapter;
import com.mikepenz.fastadapter.IClickable;
import com.mikepenz.fastadapter.IExpandable;
import com.mikepenz.fastadapter.IItem;
import com.mikepenz.fastadapter.ISubItem;
import com.mikepenz.fastadapter.expandable.items.AbstractExpandableItem;
import com.teo.ttasks.R;
import com.teo.ttasks.data.model.TTask;
import com.teo.ttasks.databinding.ItemTaskBinding;
import com.teo.ttasks.util.DateUtils;

import java.util.Comparator;
import java.util.Date;
import java.util.List;

import lombok.Getter;
import lombok.experimental.Accessors;
import timber.log.Timber;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static com.teo.ttasks.util.DateUtils.sdfDay;
import static com.teo.ttasks.util.DateUtils.sdfMonth;

@Accessors()
public class TaskItem<Parent extends IItem & IExpandable & ISubItem & IClickable> extends AbstractExpandableItem<Parent, TaskItem.ViewHolder, TaskItem<Parent>> implements Comparable<TaskItem> {

    /**
     * Comparator that sorts {@link TaskItem}s by their completion date in descending order.
     */
    public static Comparator<TaskItem> completionDateComparator = (lhs, rhs) -> {

        final boolean sameDay = sdfDay.format(lhs.completed).equals(sdfDay.format(rhs.completed));
        final boolean sameMonth = sdfMonth.format(lhs.completed).equals(sdfMonth.format(rhs.completed));

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

        // Decide whether the day number and name will be shown i.e. whether the task item will be combined or not
        if (sameDay) {
            if (returnCode > 0) {
                // rhs comes after lhs
                lhs.combineDay = true;
            } else {
                // lhs comes after rhs
                rhs.combineDay = true;
            }
        }

        // Decide whether the month should be shown
        if (sameMonth) {
            if (returnCode > 0) {
                // rhs comes after lhs
                lhs.combineMonth = true;
            } else {
                // lhs comes after rhs
                rhs.combineMonth = true;
            }
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

        final boolean sameDay = lhs.dueDate != null && rhs.dueDate != null &&
                sdfDay.format(lhs.dueDate).equals(sdfDay.format(rhs.dueDate));
        final boolean sameMonth = lhs.dueDate != null && rhs.dueDate != null &&
                sdfDay.format(lhs.dueDate).substring(4, 6).equals(sdfDay.format(rhs.dueDate).substring(4, 6));

        int returnCode;

        if (lhs.dueDate != null) {
            // Compare non-null due dates, most recent ones at the top
            if (rhs.dueDate != null) {
                returnCode = lhs.dueDate.compareTo(rhs.dueDate);
            } else {
                // This task comes after the other task
                returnCode = 1;
            }
        } else if (rhs.dueDate != null) {
            // This task comes before the other task
            returnCode = -1;
        } else {
            // Both tasks have missing due dates, they are considered equal
            returnCode = 0;
        }

        if (sameDay) {
            if (returnCode == 0 || returnCode == -1)
                lhs.combineDay = true;
            else rhs.combineDay = true;
        }
        if (sameMonth) {
            lhs.combineMonth = true;
            rhs.combineMonth = false;
        } else {
            lhs.combineMonth = true;
            rhs.combineMonth = false;
        }

        return returnCode;
    };

    @Getter private final String taskId;
    @Getter private final String title;
    @Getter private final String notes;
    @Getter private final Date dueDate;
    @Getter private final Date completed;
    @Getter private final Date reminder;

    /**
     * Flag indicating that this task item should combine its
     * day (number & name) with the previous task item in the list
     */
    @Getter private boolean combineDay;

    /**
     * Flag indicating that this task item should combine its
     * month with the previous task item in the list
     */
    @Getter private boolean combineMonth;

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
    public ViewHolder getViewHolder(@NonNull View view) {
        return new ViewHolder(view);
    }

    /**
     * Compare by due date and return results in ascending order. <br>
     * Orders with missing due dates are considered high priority and they stay at the top
     */
    @Override
    public int compareTo(@NonNull TaskItem another) {
        return dueDateComparator.compare(this, another);
    }

    public static class ViewHolder extends FastAdapter.ViewHolder<TaskItem> {

        private static final int TOP_MARGIN_DP = 12;

        public final ItemTaskBinding binding;

        final int topMargin;

        final int left, right, top, bottom;

        ViewHolder(View view) {
            super(view);
            binding = DataBindingUtil.bind(view);

            // Set the reminder icon
            Drawable reminderIcon = VectorDrawableCompat.create(view.getResources(), R.drawable.ic_alarm_18dp, view.getContext().getTheme());
            binding.reminder.setCompoundDrawablesWithIntrinsicBounds(reminderIcon, null, null, null);

            // Calculate the top margin in px
            DisplayMetrics displayMetrics = view.getContext().getResources().getDisplayMetrics();
            topMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, TOP_MARGIN_DP, displayMetrics);

            // Cache the padding
            left = binding.layoutTaskBody.getPaddingLeft();
            right = binding.layoutTaskBody.getPaddingRight();
            top = binding.layoutTaskBody.getPaddingTop();
            bottom = binding.layoutTaskBody.getPaddingBottom();
        }

        @Override
        public void bindView(@NonNull TaskItem item, @NonNull List<Object> payloads) {
            boolean combineDay = item.isCombineDay();
            boolean combineMonth = item.isCombineMonth();

            // Add the top padding for items that aren't combined
            binding.layoutTaskBody.setPadding(left,top + (!combineDay ? topMargin : 0), right, bottom);

            // Title
            binding.taskTitle.setText(item.getTitle());

            // Task description
            String notes = item.getNotes();
            if (notes == null) {
                binding.taskDescription.setVisibility(GONE);
            } else {
                binding.taskDescription.setText(notes);
                binding.taskDescription.setVisibility(VISIBLE);
            }

            // Reminder
            Date reminder = item.getReminder();
            if (reminder == null) {
                binding.reminder.setVisibility(GONE);
            } else {
                Timber.d("reminder is not null");
                binding.reminder.setText(DateUtils.formatTime(binding.getRoot().getContext(), reminder));
                binding.reminder.setVisibility(VISIBLE);
            }

            // Due date / Completed date
            Date completed = item.getCompleted();
            Date dueDate = item.getDueDate();

            if (combineDay) {
                binding.taskDate.setDate(null);
            } else {
                if (completed != null) {
                    binding.taskDate.setDate(completed);
                } else if (dueDate != null) {
                    binding.taskDate.setDate(dueDate);
                } else {
                    binding.taskDate.setDate(null);
                }
            }

            // Month
            if (combineMonth) {
                binding.month.setVisibility(GONE);
            } else {
                if (completed != null) {
                    binding.month.setText(DateUtils.getMonthAndYear(completed));
                } else if (dueDate != null) {
                    binding.month.setText(DateUtils.getMonthAndYear(dueDate));
                } else {
                    binding.month.setText("No due date");
                }
                binding.month.setVisibility(VISIBLE);
            }
        }

        @Override
        public void unbindView(@NonNull TaskItem item) {
            binding.layoutTaskBody.setPadding(0, 0, 0, 0);
            binding.taskTitle.setText(null);
            binding.taskDescription.setText(null);
            binding.reminder.setText(null);
            binding.taskDate.setDate(null);
        }
    }
}
