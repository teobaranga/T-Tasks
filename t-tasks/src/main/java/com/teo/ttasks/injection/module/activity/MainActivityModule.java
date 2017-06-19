package com.teo.ttasks.injection.module.activity;

import com.teo.ttasks.api.PeopleApi;
import com.teo.ttasks.data.local.PrefHelper;
import com.teo.ttasks.data.remote.TasksHelper;
import com.teo.ttasks.injection.module.fragment.TaskListsFragmentModule;
import com.teo.ttasks.injection.module.fragment.TasksFragmentModule;
import com.teo.ttasks.ui.activities.main.MainActivityPresenter;
import com.teo.ttasks.ui.fragments.task_lists.TaskListsFragment;
import com.teo.ttasks.ui.fragments.tasks.TasksFragment;

import dagger.Module;
import dagger.Provides;
import dagger.android.ContributesAndroidInjector;

@Module
public abstract class MainActivityModule {

    @ContributesAndroidInjector(modules = {TasksFragmentModule.class})
    abstract TasksFragment contributeTasksFragmentInjector();

    @ContributesAndroidInjector(modules = {TaskListsFragmentModule.class})
    abstract TaskListsFragment contributeTaskListsFragmentInjector();

    @Provides
    static MainActivityPresenter provideMainActivityPresenter(TasksHelper tasksHelper, PrefHelper prefHelper, PeopleApi peopleApi) {
        return new MainActivityPresenter(tasksHelper, prefHelper, peopleApi);
    }
}
