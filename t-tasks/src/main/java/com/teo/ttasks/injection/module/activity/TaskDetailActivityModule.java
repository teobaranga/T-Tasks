package com.teo.ttasks.injection.module.activity;

import com.birbit.android.jobqueue.JobManager;
import com.teo.ttasks.data.local.PrefHelper;
import com.teo.ttasks.data.local.WidgetHelper;
import com.teo.ttasks.data.remote.TasksHelper;
import com.teo.ttasks.ui.activities.task_detail.TaskDetailPresenter;
import com.teo.ttasks.util.NotificationHelper;

import dagger.Module;
import dagger.Provides;

@Module
public abstract class TaskDetailActivityModule {

    @Provides
    static TaskDetailPresenter provideTaskDetailPresenter(TasksHelper tasksHelper, PrefHelper prefHelper, WidgetHelper widgetHelper,
                                                   NotificationHelper notificationHelper, JobManager jobManager) {
        return new TaskDetailPresenter(tasksHelper, prefHelper, widgetHelper, notificationHelper, jobManager);
    }
}
