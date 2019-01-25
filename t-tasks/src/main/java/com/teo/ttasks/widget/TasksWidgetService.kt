package com.teo.ttasks.widget

import android.content.Intent
import android.widget.RemoteViewsService
import com.teo.ttasks.data.remote.TasksHelper
import dagger.android.AndroidInjection
import javax.inject.Inject

class TasksWidgetService : RemoteViewsService() {

    @Inject
    lateinit var tasksHelper: TasksHelper

    override fun onGetViewFactory(intent: Intent): RemoteViewsService.RemoteViewsFactory {
        AndroidInjection.inject(this)
        return TasksRemoteViewsFactory(applicationContext, intent, tasksHelper)
    }
}
