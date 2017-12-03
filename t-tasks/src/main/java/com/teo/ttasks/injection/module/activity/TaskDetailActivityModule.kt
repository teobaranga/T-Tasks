package com.teo.ttasks.injection.module.activity

import com.birbit.android.jobqueue.JobManager
import com.teo.ttasks.data.local.WidgetHelper
import com.teo.ttasks.data.remote.TasksHelper
import com.teo.ttasks.ui.activities.task_detail.TaskDetailPresenter
import com.teo.ttasks.util.NotificationHelper
import dagger.Module
import dagger.Provides

@Module
class TaskDetailActivityModule {

    @Provides
    internal fun provideTaskDetailPresenter(tasksHelper: TasksHelper, widgetHelper: WidgetHelper,
                                            notificationHelper: NotificationHelper, jobManager: JobManager): TaskDetailPresenter {
        return TaskDetailPresenter(tasksHelper, widgetHelper, notificationHelper, jobManager)
    }
}
