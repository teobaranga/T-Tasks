package com.teo.ttasks.widget

import android.content.Context
import android.content.Intent
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.teo.ttasks.R
import com.teo.ttasks.data.remote.TasksHelper
import com.teo.ttasks.ui.activities.task_detail.TaskDetailActivity
import com.teo.ttasks.ui.activities.task_detail.TaskDetailActivity.Companion.EXTRA_TASK_ID
import com.teo.ttasks.ui.items.TaskItem
import com.teo.ttasks.util.RxUtils
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*

class TasksRemoteViewsFactory internal constructor(context: Context, intent: Intent, private val tasksHelper: TasksHelper) : RemoteViewsService.RemoteViewsFactory {

    private val taskListId: String = intent.getStringExtra(TaskDetailActivity.EXTRA_TASK_LIST_ID)
    private val packageName: String = context.packageName
    private var taskItems: List<TaskItem>? = null

    override fun onCreate() {
        // In onCreate() you setup any connections / cursors to your data source. Heavy lifting,
        // for example downloading or creating content etc, should be deferred to onDataSetChanged()
        // or getViewAt(). Taking more than 20 seconds in this call will result in an ANR.
    }

    override fun onDataSetChanged() {

        tasksHelper.getTasks(taskListId)
                .compose(RxUtils.getTaskItems(true))
                .subscribe({ taskItems ->
                    this.taskItems = taskItems
                }, { throwable ->
                    Timber.e(throwable.toString())
                })
        Timber.d("Widget taskItems count %d", if (taskItems != null) taskItems!!.size else 0)
    }

    override fun onDestroy() {
        taskItems = null
    }

    override fun getCount(): Int {
        return taskItems?.size ?: 0
    }

    override fun getViewAt(position: Int): RemoteViews {
        val task = taskItems!![position]

        val rv = RemoteViews(packageName, R.layout.item_task_widget)

        // Title
        rv.setTextViewText(R.id.task_title, task.title)

        // Set the click action
        val intent = Intent()
        intent.putExtra(EXTRA_TASK_ID, task.taskId)
        rv.setOnClickFillInIntent(R.id.item_task_widget, intent)

        // Task description
        val notes = task.notes
        if (notes.isNullOrBlank()) {
            rv.setViewVisibility(R.id.task_description, GONE)
        } else {
            rv.setTextViewText(R.id.task_description, notes)
            rv.setViewVisibility(R.id.task_description, VISIBLE)
        }

        // Due date
        val dueDate = task.dueDate
        if (dueDate != null) {
            simpleDateFormat.applyLocalizedPattern("EEE")
            rv.setTextViewText(R.id.date_day_name, simpleDateFormat.format(dueDate))
            simpleDateFormat.applyLocalizedPattern("d")
            rv.setTextViewText(R.id.date_day_number, simpleDateFormat.format(dueDate))

            // Reminder
            val reminder = task.reminder
            if (reminder != null) {
                simpleDateFormat.applyLocalizedPattern("hh:mma")
                rv.setTextViewText(R.id.reminder, simpleDateFormat.format(reminder))
                rv.setViewVisibility(R.id.reminder, VISIBLE)
            } else {
                rv.setViewVisibility(R.id.reminder, GONE)
            }
        } else {
            rv.setTextViewText(R.id.date_day_name, null)
            rv.setTextViewText(R.id.date_day_number, null)
        }

        // Return the remote views object.
        return rv
    }

    override fun getLoadingView(): RemoteViews? {
        return null
    }

    override fun getViewTypeCount(): Int {
        return 1
    }

    override fun getItemId(position: Int): Long {
        return taskItems!![position].taskId.hashCode().toLong()
    }

    override fun hasStableIds(): Boolean {
        return true
    }

    companion object {

        private val simpleDateFormat = SimpleDateFormat("EEE", Locale.getDefault())

        init {
            simpleDateFormat.timeZone = TimeZone.getTimeZone("UTC")
        }
    }
}
