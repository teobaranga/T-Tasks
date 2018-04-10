package com.teo.ttasks.widget.configure

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import com.teo.ttasks.R
import com.teo.ttasks.data.TaskListsAdapter
import com.teo.ttasks.data.model.TTaskList
import com.teo.ttasks.databinding.ActivityWidgetConfigureBinding
import com.teo.ttasks.widget.TasksWidgetProvider
import dagger.android.support.DaggerAppCompatActivity
import javax.inject.Inject

/**
 * The configuration screen for the [TasksWidgetProvider] AppWidget.
 */
class TasksWidgetConfigureActivity : DaggerAppCompatActivity(), TasksWidgetConfigureView {

    @Inject internal lateinit var presenter: TasksWidgetConfigurePresenter

    private var mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    private lateinit var taskListsAdapter: TaskListsAdapter

    private lateinit var binding: ActivityWidgetConfigureBinding

    @Suppress("UNUSED_PARAMETER")
    fun onAddClicked(v: View) {
        // Store the task list ID associated with this widget locally
        presenter.saveWidgetTaskListId(mAppWidgetId, taskListsAdapter.getItem(binding.taskLists.selectedItemPosition).id)

        // It is the responsibility of the configuration activity to update the app widget
        // Trigger the AppWidgetProvider in order to update the widget
        val intent = Intent(this, TasksWidgetProvider::class.java)
        intent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
        // Use an array and EXTRA_APPWIDGET_IDS instead of AppWidgetManager.EXTRA_APPWIDGET_ID,
        // since it seems the onUpdate() is only fired on that:
        val ids = intArrayOf(mAppWidgetId)
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
        sendBroadcast(intent)

        // Make sure we pass back the original appWidgetId
        val resultValue = Intent()
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId)
        setResult(Activity.RESULT_OK, resultValue)
        finish()
    }

    @Suppress("UNUSED_PARAMETER")
    fun onCancelClicked(v: View) {
        finish()
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_widget_configure)
        window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        presenter.bindView(this)

        // Set the result to CANCELED.  This will cause the widget host to cancel
        // out of the widget placement if the user presses the back button.
        setResult(Activity.RESULT_CANCELED)

        // Find the widget id from the intent.
        val intent = intent
        val extras = intent.extras
        if (extras != null)
            mAppWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)

        // If this activity was started with an intent without an app widget ID, finish with an error.
        if (mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        // Set the task list adapter
        taskListsAdapter = TaskListsAdapter(this)
        taskListsAdapter.setDropDownViewResource(R.layout.spinner_item_task_list_edit_dropdown)
        binding.taskLists.adapter = taskListsAdapter

        presenter.loadTaskLists()
    }

    override fun onTaskListsLoaded(taskLists: List<TTaskList>) {
        taskListsAdapter.addAll(taskLists)
    }

    override fun onTaskListsLoadError() {
        // TODO: 2016-07-27 implement
    }

    override fun onDestroy() {
        super.onDestroy()
        presenter.unbindView(this)
    }
}
