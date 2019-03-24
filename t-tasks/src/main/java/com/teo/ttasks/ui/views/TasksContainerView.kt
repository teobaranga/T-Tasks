package com.teo.ttasks.ui.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import com.teo.ttasks.R
import com.teo.ttasks.R.layout
import com.teo.ttasks.data.model.Task
import com.teo.ttasks.util.DateUtils


/**
 */
class TasksContainerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr, defStyleRes) {

    inner class UsersAdapter(context: Context, private val tasks: List<Task>) : ArrayAdapter<Task>(context, 0, tasks) {

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: LayoutInflater.from(context).inflate(layout.item_task_inner, parent, false)

            val task = getItem(position)!!

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

    private var month: String? = null

    init {
        View.inflate(context, layout.item_tasks_container, this)

        val taskDate = findViewById<TaskDateView>(R.id.task_date)
        val monthView = findViewById<TextView>(R.id.month)
        val taskList = findViewById<ListView>(R.id.task_list)

        taskList.adapter = UsersAdapter(
            context, listOf(
                Task().apply {
                    title = "Buy groceries"
                    notes = "Go to the supermarket and pick them up"
                    reminder = "2019-01-23T02:19:56.000Z"
                },
                Task().apply {
                    title = "Study for interview"
                    notes = "Look up very complicated algorithms"
                })
        )

        val date = taskDate.date
        if (date == null) {
            month = null
            taskDate.visibility = View.GONE
            monthView.visibility = View.GONE
        } else {
            month = date.format(DateUtils.formatterMonth)
            monthView.visibility = View.VISIBLE
        }
        monthView.text = month

        taskList.addOnLayoutChangeListener(object : OnLayoutChangeListener {
            override fun onLayoutChange(
                v: View?,
                left: Int,
                top: Int,
                right: Int,
                bottom: Int,
                oldLeft: Int,
                oldTop: Int,
                oldRight: Int,
                oldBottom: Int
            ) {
                if (taskDate.bottom > taskList.bottom) {
                    with(ConstraintSet()) {
                        clone(this@TasksContainerView)

                        // Separator view
                        connect(R.id.separator, ConstraintSet.BOTTOM, R.id.task_date, ConstraintSet.BOTTOM)

                        applyTo(this@TasksContainerView)
                    }
                }
                taskList.removeOnLayoutChangeListener(this)
            }
        })

        val adapter = taskList.adapter
        if (adapter == null) {
            with(ConstraintSet()) {
                clone(this@TasksContainerView)

                // Separator view
                connect(R.id.separator, ConstraintSet.BOTTOM, R.id.task_date, ConstraintSet.BOTTOM)

                applyTo(this@TasksContainerView)
            }
        }
    }
}
