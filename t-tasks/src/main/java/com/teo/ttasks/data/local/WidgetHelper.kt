package com.teo.ttasks.data.local

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context

import com.teo.ttasks.R
import com.teo.ttasks.widget.TasksWidgetProvider

class WidgetHelper(private val context: Context, private val prefHelper: PrefHelper) {

    /**
     * Update all the widgets

     * @param taskListId task list identifier
     */
    fun updateWidgets(taskListId: String) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val ids = appWidgetManager.getAppWidgetIds(ComponentName(context, TasksWidgetProvider::class.java))
        ids.filter { taskListId == prefHelper.getWidgetTaskListId(it) }
           .forEach { appWidgetManager.notifyAppWidgetViewDataChanged(it, R.id.widget_task_list) }
    }
}
