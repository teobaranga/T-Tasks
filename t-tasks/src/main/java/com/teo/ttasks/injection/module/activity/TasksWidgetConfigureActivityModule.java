package com.teo.ttasks.injection.module.activity;

import com.teo.ttasks.data.local.PrefHelper;
import com.teo.ttasks.data.remote.TasksHelper;
import com.teo.ttasks.widget.configure.TasksWidgetConfigurePresenter;

import dagger.Module;
import dagger.Provides;

@Module
public abstract class TasksWidgetConfigureActivityModule {

    // TODO: 2016-07-27 maybe this belongs to another component
    @Provides
    static TasksWidgetConfigurePresenter provideTasksWidgetConfigurePresenter(TasksHelper tasksHelper, PrefHelper prefHelper) {
        return new TasksWidgetConfigurePresenter(tasksHelper, prefHelper);
    }
}
