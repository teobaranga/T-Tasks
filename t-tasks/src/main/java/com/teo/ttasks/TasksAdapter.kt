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
) : RecyclerView.Adapter<TasksAdapter.ViewHolder>() {

    companion object {
        private val currentYear by lazy { ZonedDateTime.now().year }
    }

    interface TaskClickListener {
        fun onTaskClicked(task: Task)
    }

    enum class DateType(val accessFunction: Task.() -> ZonedDateTime?) {
        COMPLETED({ completedDate }),
        DUE({ dueDate });
    }

    abstract class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        abstract fun bind(position: Int)
    }

    inner class TopViewHolder(itemView: View) : ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.section_title)
        val completed: String = itemView.context.getString(R.string.task_section_completed)
        val active: String = itemView.context.getString(R.string.task_section_active)

        override fun bind(position: Int) {
            if (activeTasks.isEmpty() || position != 0) {
                title.text = completed
                title.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_done_24dp, 0, 0, 0)
            } else {
                title.text = active
                title.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_whatshot_24dp, 0, 0, 0)
            }
        }
    }

    inner class MiddleViewHolder(itemView: View) : ViewHolder(itemView), View.OnClickListener {

        private val dayView: TaskDateView = itemView.findViewById(R.id.task_date)
        private val monthView: TextView = itemView.findViewById(R.id.month)
        private val title: TextView = itemView.findViewById(R.id.task_title)
        private val description: TextView = itemView.findViewById(R.id.task_description)
        private val reminder: TextView = itemView.findViewById(R.id.reminder)
        private val taskBodyLayout: View = itemView.findViewById(R.id.layout_task_body)

        private lateinit var task: Task

        init {
            taskBodyLayout.setOnClickListener(this)
        }

        override fun bind(position: Int) {
            val (task, prevTask, dateType) = when {
                position == 0 -> throw IllegalArgumentException("Not supposed to be 0 and MIDDLE")
                position <= activeTasks.size -> Triple(
                    activeTasks[position - 1],
                    activeTasks.getOrNull(position - 2),
                    DateType.DUE
                )
                else -> Triple(
                    completedTasks[position - activeTasks.size - 3],
                    completedTasks.getOrNull(position - activeTasks.size - 4),
                    DateType.COMPLETED
                )
            }

            this.task = task

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

        override fun onClick(v: View?) {
            val adapterPosition = adapterPosition
            taskClickListener?.let {
                if (adapterPosition != RecyclerView.NO_POSITION) {

                    it.onTaskClicked(task)
                }
            }
        }
    }

    inner class BottomViewHolder(itemView: View) : ViewHolder(itemView) {
        override fun bind(position: Int) {
            // Nothing to do
        }
    }

    var taskClickListener: TaskClickListener? = null

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

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(position)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
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
