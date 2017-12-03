package com.teo.ttasks.injection.module.fragment

import com.teo.ttasks.data.remote.TasksHelper
import com.teo.ttasks.ui.fragments.task_lists.TaskListsPresenter

import dagger.Module
import dagger.Provides

@Module
class TaskListsFragmentModule {

    @Provides
    internal fun provideTaskListsPresenter(tasksHelper: TasksHelper): TaskListsPresenter {
        return TaskListsPresenter(tasksHelper)
    }
}
