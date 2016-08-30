package com.teo.ttasks.data.local;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;

import com.teo.ttasks.R;
import com.teo.ttasks.widget.TasksWidgetProvider;

public class WidgetHelper {

    private final Context context;

    public WidgetHelper(Context applicationContext) {
        context = applicationContext;
    }

    /**
     * Update all the widgets
     *
     * @param taskListId task list identifier
     */
    public void updateWidgets(String taskListId) {
        final AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        int[] ids = appWidgetManager.getAppWidgetIds(new ComponentName(context, TasksWidgetProvider.class));
        PrefHelper prefHelper = new PrefHelper(context);
        for (int id : ids) {
            if (taskListId.equals(prefHelper.getWidgetTaskListId(id)))
                appWidgetManager.notifyAppWidgetViewDataChanged(id, R.id.widget_task_list);
        }
    }
}
