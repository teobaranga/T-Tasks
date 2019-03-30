package com.teo.ttasks.ui.views

import android.content.Context
import android.content.res.Resources
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.updateMargins
import com.teo.ttasks.R
import com.teo.ttasks.R.layout
import com.teo.ttasks.data.model.Task
import com.teo.ttasks.util.DateUtils
import org.threeten.bp.ZonedDateTime

private val Int.dpToPx: Int
    get() = (this * Resources.getSystem().displayMetrics.density).toInt()

private val DEFAULT_MARGIN = 8.dpToPx

/**
 */
class TasksContainerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr, defStyleRes) {

    var date: ZonedDateTime? = null
        set(value) {
            if (value == null) {
                taskDateView.visibility = View.GONE
                monthView.visibility = View.GONE
            } else {
                with(taskDateView) {
                    date = value
                    visibility = View.VISIBLE
                }
                with(monthView) {
                    text = value.format(DateUtils.formatterMonth)
                    visibility = View.VISIBLE
                }
            }
            field = value
        }

    var tasks: List<Task>? = null
        set(tasks) {
            if (!tasks.isNullOrEmpty()) {
                val lastIndex = tasks.size - 1
                for ((index, task) in tasks.withIndex()) {
                    val taskView = getInnerTaskItemView(task)
                    if (index < lastIndex) {
                        taskView.layoutParams = (taskView.layoutParams as LinearLayout.LayoutParams).apply {
                            updateMargins(bottom = DEFAULT_MARGIN)
                        }
                    }
                    taskView.setOnClickListener { v -> println("click") }
                    taskListView.addView(taskView)
                }

                with(ConstraintSet()) {
                    clone(this@TasksContainerView)

                    // Separator view
                    connect(R.id.separator, ConstraintSet.BOTTOM, R.id.task_list, ConstraintSet.BOTTOM)

                    applyTo(this@TasksContainerView)
                }
            } else {
                taskListView.removeAllViewsInLayout()

                with(ConstraintSet()) {
                    clone(this@TasksContainerView)

                    // Separator view
                    connect(R.id.separator, ConstraintSet.BOTTOM, R.id.task_date, ConstraintSet.BOTTOM)

                    applyTo(this@TasksContainerView)
                }
            }
            field = tasks
        }

    private val taskDateView: TaskDateView

    private val monthView: TextView

    private val taskListView: LinearLayout

    init {
        View.inflate(context, layout.item_tasks_container, this)

        taskDateView = findViewById(R.id.task_date)
        monthView = findViewById(R.id.month)
        taskListView = findViewById(R.id.task_list)
    }

    private fun getInnerTaskItemView(task: Task): View {
        val view = LayoutInflater.from(context).inflate(layout.item_task_inner, taskListView, false)
        val title = view.findViewById<TextView>(R.id.task_title)
        val description = view.findViewById<TextView>(R.id.task_description)
        val reminder = view.findViewById<TextView>(R.id.reminder)

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
        return view
    }
}
