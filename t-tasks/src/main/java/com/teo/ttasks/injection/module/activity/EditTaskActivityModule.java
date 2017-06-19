package com.teo.ttasks.injection.module.activity;

import com.birbit.android.jobqueue.JobManager;
import com.teo.ttasks.data.local.PrefHelper;
import com.teo.ttasks.data.local.WidgetHelper;
import com.teo.ttasks.data.remote.TasksHelper;
import com.teo.ttasks.ui.activities.edit_task.EditTaskPresenter;
import com.teo.ttasks.util.NotificationHelper;

import dagger.Module;
import dagger.Provides;

@Module
public abstract class EditTaskActivityModule {

    @Provides
    static EditTaskPresenter provideEditTaskPresenter(TasksHelper tasksHelper, PrefHelper prefHelper, WidgetHelper widgetHelper,
                                               NotificationHelper notificationHelper, JobManager jobManager) {
        return new EditTaskPresenter(tasksHelper, prefHelper, widgetHelper, notificationHelper, jobManager);
    }
}
