package com.teo.ttasks.injection.module.activity

import com.birbit.android.jobqueue.JobManager
import com.teo.ttasks.data.local.WidgetHelper
import com.teo.ttasks.data.remote.TasksHelper
import com.teo.ttasks.ui.activities.edit_task.EditTaskPresenter
import com.teo.ttasks.util.NotificationHelper

import dagger.Module
import dagger.Provides

@Module
class EditTaskActivityModule {

    @Provides
    internal fun provideEditTaskPresenter(tasksHelper: TasksHelper, widgetHelper: WidgetHelper,
                                          notificationHelper: NotificationHelper, jobManager: JobManager): EditTaskPresenter {
        return EditTaskPresenter(tasksHelper, widgetHelper, notificationHelper, jobManager)
    }
}
