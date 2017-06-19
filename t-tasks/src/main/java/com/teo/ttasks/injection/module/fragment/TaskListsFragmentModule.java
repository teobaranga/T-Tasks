package com.teo.ttasks.injection.module.fragment;

import com.teo.ttasks.data.remote.TasksHelper;
import com.teo.ttasks.ui.fragments.task_lists.TaskListsPresenter;

import dagger.Module;
import dagger.Provides;

@Module
public abstract class TaskListsFragmentModule {

    @Provides
    static TaskListsPresenter provideTaskListsPresenter(TasksHelper tasksHelper) {
        return new TaskListsPresenter(tasksHelper);
    }
}
