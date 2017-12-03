package com.teo.ttasks.injection.module.fragment

import com.teo.ttasks.data.local.PrefHelper
import com.teo.ttasks.data.remote.TasksHelper
import com.teo.ttasks.ui.fragments.tasks.TasksPresenter

import dagger.Module
import dagger.Provides

@Module
class TasksFragmentModule {

    @Provides
    internal fun provideTasksPresenter(tasksHelper: TasksHelper, prefHelper: PrefHelper): TasksPresenter {
        return TasksPresenter(tasksHelper, prefHelper)
    }
}
