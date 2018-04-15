package com.teo.ttasks.ui.items

import android.support.graphics.drawable.VectorDrawableCompat
import android.support.v7.widget.RecyclerView
import android.util.TypedValue
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import com.teo.ttasks.R
import com.teo.ttasks.data.model.Task
import com.teo.ttasks.databinding.ItemTaskBinding
import com.teo.ttasks.util.DateUtils
import com.teo.ttasks.util.DateUtils.Companion.sdfDay
import com.teo.ttasks.util.DateUtils.Companion.sdfMonth
import com.teo.ttasks.util.TaskType
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.AbstractFlexibleItem
import eu.davidea.flexibleadapter.items.IFlexible
import eu.davidea.viewholders.FlexibleViewHolder
import java.util.*


class TaskItem(task: Task,
               private val taskType: TaskType)
    : AbstractFlexibleItem<TaskItem.ViewHolder>(), Comparable<TaskItem> {

    val taskId: String = task.id

    val title: String = task.title!!

    val notes: String? = task.notes

    val dueDate: Date? = task.due

    val completed: Date? = task.completed

    val reminder: Date? = task.reminder

    private lateinit var viewHolder: ViewHolder

    val binding: ItemTaskBinding
        get() = viewHolder.binding

    /**
     * Flag indicating that this task item should combine its
     * day (number & name) with the previous task item in the list
     */
    private var combineDay: Boolean = false

    /**
     * Flag indicating that this task item should combine its
     * month with the previous task item in the list
     */
    private var combineMonth: Boolean = false

    override fun shouldNotifyChange(newItem: IFlexible<*>?): Boolean =
    // Should be bound again if ID is different
            taskId != (newItem as TaskItem).taskId

    override fun getItemViewType(): Int = taskType.id

    override fun getLayoutRes(): Int = R.layout.item_task

    override fun bindViewHolder(adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>, viewHolder: ViewHolder, position: Int, payloads: MutableList<Any?>) {
        this.viewHolder = viewHolder
        val binding = viewHolder.binding

        // Add the top padding for items that aren't combined
        binding.layoutTaskBody.setPadding(viewHolder.left,
                viewHolder.top + if (!combineDay) viewHolder.topMargin else 0,
                viewHolder.right,
                viewHolder.bottom)

        // Title
        binding.taskTitle.text = title

        // Task description
        if (notes == null) {
            binding.taskDescription.visibility = GONE
        } else {
            binding.taskDescription.text = notes
            binding.taskDescription.visibility = VISIBLE
        }

        // Reminder
        if (reminder == null) {
            binding.reminder.visibility = GONE
        } else {
            binding.reminder.text = DateUtils.formatTime(binding.root.context, reminder)
            binding.reminder.visibility = VISIBLE
        }

        // Due date / Completed date
        if (combineDay) {
            binding.taskDate.date = null
        } else {
            when {
                completed != null -> binding.taskDate.date = completed
                dueDate != null -> binding.taskDate.date = dueDate
                else -> binding.taskDate.date = null
            }
        }

        // Month
        if (combineMonth) {
            binding.month.visibility = GONE
        } else {
            when {
                completed != null -> binding.month.text = DateUtils.getMonthAndYear(completed)
                dueDate != null -> binding.month.text = DateUtils.getMonthAndYear(dueDate)
                else -> binding.month.setText(R.string.due_date_not_set)
            }
            binding.month.visibility = VISIBLE
        }
    }

    override fun createViewHolder(view: View, adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>): ViewHolder {
        return ViewHolder(view, adapter)
    }

    /**
     * Compare by due date and return results in ascending order. <br></br>
     * Orders with missing due dates are considered high priority and they stay at the top
     */
    override fun compareTo(other: TaskItem): Int = dueDateComparator.compare(this, other)

    override fun equals(other: Any?): Boolean =
            other is TaskItem &&
                    taskId == other.taskId &&
                    title == other.title &&
                    notes == other.notes &&
                    dueDate == other.dueDate &&
                    completed == other.completed &&
                    reminder == other.reminder

    override fun hashCode(): Int {
        var result = taskId.hashCode()
        result = 37 * result + title.hashCode()
        notes?.let { result = 37 * result + it.hashCode() }
        dueDate?.let { result = 37 * result + it.hashCode() }
        completed?.let { result = 37 * result + it.hashCode() }
        reminder?.let { result = 37 * result + it.hashCode() }
        return result
    }

    class ViewHolder internal constructor(view: View, adapter: FlexibleAdapter<*>) : FlexibleViewHolder(view, adapter) {

        val binding = ItemTaskBinding.bind(view)

        internal val topMargin: Int

        internal val left: Int
        internal val right: Int
        internal val top: Int
        internal val bottom: Int

        init {

            // Set the reminder icon
            val reminderIcon = VectorDrawableCompat.create(view.resources, R.drawable.ic_alarm_18dp, view.context.theme)
            binding.reminder.setCompoundDrawablesWithIntrinsicBounds(reminderIcon, null, null, null)

            // Calculate the top margin in px
            val displayMetrics = view.context.resources.displayMetrics
            topMargin = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, TOP_MARGIN_DP.toFloat(), displayMetrics).toInt()

            // Cache the padding
            left = binding.layoutTaskBody.paddingLeft
            right = binding.layoutTaskBody.paddingRight
            top = binding.layoutTaskBody.paddingTop
            bottom = binding.layoutTaskBody.paddingBottom
        }

        companion object {
            private const val TOP_MARGIN_DP = 12
        }
    }

    companion object {

        /**
         * Comparator that sorts [TaskItem]s by their completion date in descending order.
         */
        var completionDateComparator = Comparator<TaskItem> { lhs, rhs ->

            val sameDay = sdfDay.format(lhs.completed) == sdfDay.format(rhs.completed)
            val sameMonth = sdfMonth.format(lhs.completed) == sdfMonth.format(rhs.completed)

            // Check the completed date
            val returnCode =
                    if (lhs.completed == null && rhs.completed == null) {
                        0
                    } else if (lhs.completed == null) {
                        1
                    } else if (rhs.completed == null) {
                        -1
                    } else {
                        rhs.completed.compareTo(lhs.completed)
                    }

            // Decide whether the day number and name will be shown i.e. whether the task item will be combined or not
            if (sameDay) {
                if (returnCode > 0) {
                    // rhs comes after lhs
                    lhs.combineDay = true
                } else {
                    // lhs comes after rhs
                    rhs.combineDay = true
                }
            }

            // Decide whether the month should be shown
            if (sameMonth) {
                if (returnCode > 0) {
                    // rhs comes after lhs
                    lhs.combineMonth = true
                } else {
                    // lhs comes after rhs
                    rhs.combineMonth = true
                }
            }

            return@Comparator returnCode
        }

        var alphabeticalComparator = Comparator<TaskItem> { lhs, rhs ->
            // Titles should never be null or empty but just in case
            val noTitleLeft = lhs.title.isEmpty()
            val noTitleRight = rhs.title.isEmpty()
            if (noTitleLeft && noTitleRight)
                return@Comparator 0
            if (noTitleLeft)
                return@Comparator -1
            if (noTitleRight)
                return@Comparator 1
            return@Comparator lhs.title.compareTo(rhs.title, ignoreCase = true)
        }

        /**
         * Comparator that sorts [TaskItem]s by their due dates in ascending order. Default comparator.
         */
        private val dueDateComparator = Comparator<TaskItem> { lhs, rhs ->

            val sameDay = lhs.dueDate != null && rhs.dueDate != null &&
                    sdfDay.format(lhs.dueDate) == sdfDay.format(rhs.dueDate)
            val sameMonth = lhs.dueDate != null && rhs.dueDate != null &&
                    sdfDay.format(lhs.dueDate).substring(4, 6) == sdfDay.format(rhs.dueDate).substring(4, 6)

            val returnCode =
                    if (lhs.dueDate != null) {
                        // Compare non-null due dates, most recent ones at the top
                        if (rhs.dueDate != null) {
                            lhs.dueDate.compareTo(rhs.dueDate)
                        } else {
                            // This task comes after the other task
                            1
                        }
                    } else if (rhs.dueDate != null) {
                        // This task comes before the other task
                        -1
                    } else {
                        // Both tasks have missing due dates, they are considered equal
                        0
                    }

            if (sameDay) {
                if (returnCode == 0 || returnCode == -1)
                    lhs.combineDay = true
                else
                    rhs.combineDay = true
            }
            if (sameMonth) {
                lhs.combineMonth = true
                rhs.combineMonth = false
            } else {
                lhs.combineMonth = true
                rhs.combineMonth = false
            }

            return@Comparator returnCode
        }
    }
}
