package com.teo.ttasks

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.teo.ttasks.data.model.Task
import com.teo.ttasks.ui.views.TaskDateView
import com.teo.ttasks.util.DateUtils
import org.threeten.bp.ZonedDateTime

const val VIEW_TYPE_TOP = 0
const val VIEW_TYPE_MIDDLE = 1
const val VIEW_TYPE_BOTTOM = 2

class TasksAdapter(
    var activeTasks: List<Task> = emptyList(),
    var completedTasks: List<Task> = emptyList()
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private val currentYear by lazy { ZonedDateTime.now().year }
    }

    enum class DateType(val accessFunction: Task.() -> ZonedDateTime?) {
        COMPLETED({ completedDate }),
        DUE({ dueDate });
    }

    inner class TopViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.section_title)
        val sortType: TextView = itemView.findViewById(R.id.sort_type)
    }

    inner class MiddleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val dayView: TaskDateView = itemView.findViewById(R.id.task_date)
        private val monthView: TextView = itemView.findViewById(R.id.month)
        private val title: TextView = itemView.findViewById(R.id.task_title)
        private val description: TextView = itemView.findViewById(R.id.task_description)
        private val reminder: TextView = itemView.findViewById(R.id.reminder)

        fun compute(task:Task, prevTask: Task?, dateType: DateType) {
            val getSortDate: Task.() -> ZonedDateTime? = dateType.accessFunction
            val taskSortDate = task.getSortDate()
            val prevTaskSortDate = prevTask?.getSortDate()

            with(dayView) {
                val showDay = prevTaskSortDate?.dayOfMonth != taskSortDate?.dayOfMonth
                if (showDay) {
                    date = taskSortDate
                    visibility = View.VISIBLE
                } else {
                    date = null
                    visibility = View.GONE
                }
            }

            with(monthView) {
                val showMonth = prevTaskSortDate?.month != taskSortDate?.month
                if (showMonth && taskSortDate != null) {
                    text = when (currentYear) {
                        taskSortDate.year -> taskSortDate.format(DateUtils.formatterMonth)
                        else -> taskSortDate.format(DateUtils.formatterMonthYear)
                    }
                    visibility = View.VISIBLE
                } else {
                    visibility = View.GONE
                }
            }

            title.text = task.title

            if (task.notes.isNullOrBlank()) {
                description.visibility = View.GONE
            } else {
                description.text = task.notes
                description.visibility = View.VISIBLE
            }

            if (task.reminderDate == null) {
                reminder.visibility = View.GONE
            } else {
                reminder.text = task.reminderDate!!.format(DateUtils.formatterTime)
                reminder.visibility = View.VISIBLE
            }
        }
    }

    inner class BottomViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    override fun getItemViewType(position: Int): Int {
        if (activeTasks.isEmpty() && completedTasks.isEmpty()) {
            return VIEW_TYPE_MIDDLE
        }

        if (position == 0) {
            return VIEW_TYPE_TOP
        }

        if (position == itemCount - 1) {
            return VIEW_TYPE_BOTTOM
        }

        if (activeTasks.isEmpty() || completedTasks.isEmpty()) {
            return VIEW_TYPE_MIDDLE
        }

        if (position == activeTasks.size + 1) {
            return VIEW_TYPE_BOTTOM
        }

        if (position == activeTasks.size + 2) {
            return VIEW_TYPE_TOP
        }

        return VIEW_TYPE_MIDDLE
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        with(recyclerView.recycledViewPool) {
            setMaxRecycledViews(VIEW_TYPE_TOP, 2)
            setMaxRecycledViews(VIEW_TYPE_MIDDLE, 15)
            setMaxRecycledViews(VIEW_TYPE_BOTTOM, 2)
        }

    }

    override fun getItemCount(): Int {
        return (if (activeTasks.isEmpty()) 0 else activeTasks.size + 2) +
                (if (completedTasks.isEmpty()) 0 else completedTasks.size + 2)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder.itemViewType) {
            VIEW_TYPE_TOP -> {
                val topViewHolder = holder as TopViewHolder
                if (activeTasks.isEmpty() || position != 0) {
                    topViewHolder.title.text = "Completed"
                    topViewHolder.title.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_done_24dp, 0, 0, 0)
                } else {
                    topViewHolder.title.text = "Active"
                    topViewHolder.title.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_whatshot_24dp, 0, 0, 0)
                }
            }
            VIEW_TYPE_MIDDLE -> {
                val (task, prevTask, dateType) = when {
                    position == 0 -> throw IllegalArgumentException("Not supposed to be 0 and MIDDLE")
                    position <= activeTasks.size -> Triple(activeTasks[position - 1], activeTasks.getOrNull(position - 2), DateType.DUE)
                    else -> Triple(completedTasks[position - activeTasks.size - 3], completedTasks.getOrNull(position - activeTasks.size - 4), DateType.COMPLETED)
                }
                (holder as MiddleViewHolder).compute(task, prevTask, dateType)
            }
            VIEW_TYPE_BOTTOM -> {

            }
            else -> throw IllegalArgumentException("Unsupported viewType: ${holder.itemViewType}")
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_TOP ->
                TopViewHolder(layoutInflater.inflate(R.layout.task_section_top, parent, false))
            VIEW_TYPE_MIDDLE ->
                MiddleViewHolder(layoutInflater.inflate(R.layout.task_section_middle, parent, false))
            VIEW_TYPE_BOTTOM ->
                BottomViewHolder(layoutInflater.inflate(R.layout.task_section_bottom, parent, false))
            else -> throw IllegalArgumentException("Unsupported viewType: $viewType")
        }
    }
}
