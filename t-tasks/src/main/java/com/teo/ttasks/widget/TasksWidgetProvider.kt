package com.teo.ttasks.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import com.teo.ttasks.R
import com.teo.ttasks.data.local.PrefHelper
import com.teo.ttasks.data.model.TaskList
import com.teo.ttasks.ui.activities.edit_task.EditTaskActivity
import com.teo.ttasks.ui.activities.main.MainActivity
import com.teo.ttasks.ui.activities.task_detail.TaskDetailActivity
import com.teo.ttasks.widget.configure.TasksWidgetConfigureActivity
import dagger.android.AndroidInjection
import io.realm.Realm
import timber.log.Timber
import java.util.*
import javax.inject.Inject

/**
 * Implementation of App Widget functionality.
 * App Widget Configuration implemented in [TasksWidgetConfigureActivity]
 */
class TasksWidgetProvider : AppWidgetProvider() {

    @Inject internal lateinit var prefHelper: PrefHelper

    override fun onReceive(context: Context?, intent: Intent?) {
        AndroidInjection.inject(this, context)
        super.onReceive(context, intent)
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        Timber.d("onUpdate with ids %s", Arrays.toString(appWidgetIds))
        val realm = Realm.getDefaultInstance()
        // There may be multiple widgets active, so update all of them
        for (appWidgetId in appWidgetIds) {
            if (prefHelper.getWidgetTaskListId(appWidgetId) != null)
                updateAppWidget(context, appWidgetManager, appWidgetId, realm)
            else
                Timber.d("widget id %d not found", appWidgetId)
        }
        realm.close()
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        // When the user deletes the widget, delete the preference associated with it.
        for (appWidgetId in appWidgetIds) {
            prefHelper.deleteWidgetTaskId(appWidgetId)
        }
    }

    override fun onEnabled(context: Context) {
        // Enter relevant functionality for when the first widget is created
    }

    override fun onDisabled(context: Context) {
        // Enter relevant functionality for when the last widget is disabled
    }

    private fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, realm: Realm) {
        Timber.d("updating widget with id %d", appWidgetId)

        val taskListId = prefHelper.getWidgetTaskListId(appWidgetId)

        // Set up the intent that starts the TasksWidgetService, which will
        // provide the views for this collection.
        val intent = Intent(context, TasksWidgetService::class.java)

        intent.putExtra(TaskDetailActivity.EXTRA_TASK_LIST_ID, taskListId)

        // Add the app widget ID to the intent extras.
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        intent.data = Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME))

        // Instantiate the RemoteViews object for the app widget layout.
        val views = RemoteViews(context.packageName, R.layout.tasks_widget)
        // Set the background color programmatically
        views.setInt(R.id.container, "setBackgroundResource", R.color.background_color)

        // Set up the RemoteViews object to use a RemoteViews adapter.
        // This adapter connects to a RemoteViewsService through the specified intent.
        // This is how you populate the data.
        views.setRemoteAdapter(R.id.widget_task_list, intent)

        // The empty view is displayed when the collection has no items.
        // It should be in the same layout used to instantiate the RemoteViews
        // object above.
        //        views.setEmptyView(R.id.stack_view, R.id.empty_view);

        // This section makes it possible for items to have individualized behavior.
        // It does this by setting up a pending intent template. Individuals items of a collection
        // cannot set up their own pending intents. Instead, the collection as a whole sets
        // up a pending intent template, and the individual items set a fillInIntent
        // to create unique behavior on an item-by-item basis.
        val taskDetailIntent = TaskDetailActivity.getIntentTemplate(context, taskListId!!)
        taskDetailIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        val taskDetailPendingIntent = PendingIntent.getActivity(context, 0, taskDetailIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        views.setPendingIntentTemplate(R.id.widget_task_list, taskDetailPendingIntent)

        //
        // Do additional processing specific to this app widget...
        //

        val taskList = realm.where(TaskList::class.java)
                .equalTo("id", taskListId)
                .findFirst()

        // The task list can be null when upgrading the Realm scheme, prevent crashing
        if (taskList != null) {
            // Set the task list title
            views.setTextViewText(R.id.task_list_title, taskList.title)
        }

        // Setup the header
        val viewTaskListIntent = Intent(context, MainActivity::class.java)
        viewTaskListIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        val viewTaskListPendingIntent = PendingIntent.getActivity(context, 0, viewTaskListIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        views.setOnClickPendingIntent(R.id.task_list_header, viewTaskListPendingIntent)

        // Setup the Add Task button
        // Set the icon
        views.setImageViewResource(R.id.add_task, R.drawable.ic_add_white_24dp)
        // Set the click action
        val addTaskIntent = EditTaskActivity.getTaskCreateIntent(context, taskListId)
        addTaskIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        val pendingIntent = PendingIntent.getActivity(context, 0, addTaskIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        views.setOnClickPendingIntent(R.id.add_task, pendingIntent)

        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}
