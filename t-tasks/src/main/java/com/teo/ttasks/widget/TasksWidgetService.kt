package com.teo.ttasks.widget

import android.content.Intent
import android.widget.RemoteViewsService
import com.teo.ttasks.data.remote.TasksHelper
import org.koin.android.ext.android.inject

class TasksWidgetService : RemoteViewsService() {

    private val tasksHelper: TasksHelper by inject()

    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return TasksRemoteViewsFactory(applicationContext, intent, tasksHelper)
    }
}
