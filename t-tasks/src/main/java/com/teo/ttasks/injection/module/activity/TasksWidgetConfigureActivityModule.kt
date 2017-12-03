package com.teo.ttasks.injection.module.activity

import com.teo.ttasks.data.local.PrefHelper
import com.teo.ttasks.data.remote.TasksHelper
import com.teo.ttasks.widget.configure.TasksWidgetConfigurePresenter

import dagger.Module
import dagger.Provides

@Module
class TasksWidgetConfigureActivityModule {

    // TODO: 2016-07-27 maybe this belongs to another component
    @Provides
    internal fun provideTasksWidgetConfigurePresenter(tasksHelper: TasksHelper, prefHelper: PrefHelper): TasksWidgetConfigurePresenter {
        return TasksWidgetConfigurePresenter(tasksHelper, prefHelper)
    }
}
