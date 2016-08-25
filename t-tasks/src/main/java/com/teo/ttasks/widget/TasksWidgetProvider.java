package com.teo.ttasks.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.RemoteViews;

import com.teo.ttasks.R;
import com.teo.ttasks.TTasksApp;
import com.teo.ttasks.data.local.PrefHelper;
import com.teo.ttasks.data.model.TaskList;
import com.teo.ttasks.ui.activities.edit_task.EditTaskActivity;
import com.teo.ttasks.ui.activities.main.MainActivity;
import com.teo.ttasks.ui.activities.task_detail.TaskDetailActivity;
import com.teo.ttasks.widget.configure.TasksWidgetConfigureActivity;

import java.util.Arrays;

import javax.inject.Inject;

import io.realm.Realm;
import timber.log.Timber;

/**
 * Implementation of App Widget functionality.
 * App Widget Configuration implemented in {@link TasksWidgetConfigureActivity TasksWidgetConfigureActivity}
 */
public class TasksWidgetProvider extends AppWidgetProvider {

    @Inject PrefHelper prefHelper;

    /**
     * Update all the widgets
     *
     * @param context    context
     * @param taskListId task list identifier
     */
    public static void updateWidgets(Context context, String taskListId) {
        final AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        int[] ids = appWidgetManager.getAppWidgetIds(new ComponentName(context, TasksWidgetProvider.class));
        PrefHelper prefHelper = new PrefHelper(context);
        for (int id : ids) {
            if (taskListId.equals(prefHelper.getWidgetTaskListId(id)))
                appWidgetManager.notifyAppWidgetViewDataChanged(id, R.id.widget_task_list);
        }
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Timber.d("onUpdate with ids %s", Arrays.toString(appWidgetIds));
        if (prefHelper == null)
            TTasksApp.get(context).userComponent().inject(this);
        Realm realm = Realm.getDefaultInstance();
        // There may be multiple widgets active, so update all of them
        for (int appWidgetId : appWidgetIds) {
            if (prefHelper.getWidgetTaskListId(appWidgetId) != null)
                updateAppWidget(context, appWidgetManager, appWidgetId, realm);
            else Timber.d("widget id %d not found", appWidgetId);
        }
        realm.close();
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        if (prefHelper == null)
            TTasksApp.get(context).userComponent().inject(this);
        // When the user deletes the widget, delete the preference associated with it.
        for (int appWidgetId : appWidgetIds) {
            prefHelper.deleteWidgetTaskId(appWidgetId);
        }
    }

    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
    }

    private void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId, Realm realm) {
        Timber.d("updating widget with id %d", appWidgetId);

        String taskListId = prefHelper.getWidgetTaskListId(appWidgetId);

        // Set up the intent that starts the TasksWidgetService, which will
        // provide the views for this collection.
        Intent intent = new Intent(context, TasksWidgetService.class);

        intent.putExtra(TaskDetailActivity.EXTRA_TASK_LIST_ID, taskListId);

        // Add the app widget ID to the intent extras.
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));

        // Instantiate the RemoteViews object for the app widget layout.
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.tasks_widget);

        // Set up the RemoteViews object to use a RemoteViews adapter.
        // This adapter connects to a RemoteViewsService  through the specified intent.
        // This is how you populate the data.
        views.setRemoteAdapter(R.id.widget_task_list, intent);

        // The empty view is displayed when the collection has no items.
        // It should be in the same layout used to instantiate the RemoteViews
        // object above.
//        views.setEmptyView(R.id.stack_view, R.id.empty_view);

        // This section makes it possible for items to have individualized behavior.
        // It does this by setting up a pending intent template. Individuals items of a collection
        // cannot set up their own pending intents. Instead, the collection as a whole sets
        // up a pending intent template, and the individual items set a fillInIntent
        // to create unique behavior on an item-by-item basis.
        Intent taskDetailIntent = TaskDetailActivity.getIntentTemplate(context, taskListId);
        taskDetailIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent taskDetailPendingIntent = PendingIntent.getActivity(context, 0, taskDetailIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        views.setPendingIntentTemplate(R.id.widget_task_list, taskDetailPendingIntent);

        //
        // Do additional processing specific to this app widget...
        //

        TaskList taskList = realm.where(TaskList.class)
                .equalTo("id", taskListId)
                .findFirst();

        // The task list can be null when upgrading the Realm scheme
        if (taskList != null) {
            // Set the task list title
            views.setTextViewText(R.id.task_list_title, taskList.getTitle());
        }
        // TODO: 2016-08-24 handle null task list

        // Setup the header
        Intent viewTaskListIntent = new Intent(context, MainActivity.class);
        viewTaskListIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent viewTaskListPendingIntent = PendingIntent.getActivity(context, 0, viewTaskListIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        views.setOnClickPendingIntent(R.id.task_list_header, viewTaskListPendingIntent);

        // Setup the Add Task button
        // Set the icon
        views.setImageViewResource(R.id.add_task, R.drawable.ic_add_white_24dp);
        // Set the click action
        Intent addTaskIntent = EditTaskActivity.getTaskCreateIntent(context, taskListId);
        addTaskIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, addTaskIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        views.setOnClickPendingIntent(R.id.add_task, pendingIntent);

        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }
}
