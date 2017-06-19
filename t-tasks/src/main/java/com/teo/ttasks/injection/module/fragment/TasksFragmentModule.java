package com.teo.ttasks.injection.module.fragment;

import com.teo.ttasks.data.local.PrefHelper;
import com.teo.ttasks.data.remote.TasksHelper;
import com.teo.ttasks.ui.fragments.tasks.TasksPresenter;

import dagger.Module;
import dagger.Provides;

@Module
public abstract class TasksFragmentModule {

    @Provides
    static TasksPresenter provideTasksPresenter(TasksHelper tasksHelper, PrefHelper prefHelper) {
        return new TasksPresenter(tasksHelper, prefHelper);
    }
}
