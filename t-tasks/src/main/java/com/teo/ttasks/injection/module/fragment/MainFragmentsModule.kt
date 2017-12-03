package com.teo.ttasks.injection.module.fragment

import com.teo.ttasks.ui.fragments.task_lists.TaskListsFragment
import com.teo.ttasks.ui.fragments.tasks.TasksFragment
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
abstract class MainFragmentsModule {

    @ContributesAndroidInjector(modules = arrayOf(TasksFragmentModule::class))
    abstract fun contributeTasksFragmentInjector(): TasksFragment

    @ContributesAndroidInjector(modules = arrayOf(TaskListsFragmentModule::class))
    abstract fun contributeTaskListsFragmentInjector(): TaskListsFragment
}