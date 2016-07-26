package com.teo.ttasks.injection.module;

import com.teo.ttasks.data.local.PrefHelper;
import com.teo.ttasks.data.remote.TasksHelper;
import com.teo.ttasks.ui.activities.edit_task.EditTaskPresenter;
import com.teo.ttasks.ui.activities.main.MainActivityPresenter;
import com.teo.ttasks.ui.activities.task_detail.TaskDetailPresenter;
import com.teo.ttasks.ui.fragments.tasks.TasksPresenter;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class UserModule {

    @Provides @Singleton
    TasksPresenter provideTasksPresenter(TasksHelper tasksHelper) {
        return new TasksPresenter(tasksHelper);
    }

    @Provides @Singleton
    MainActivityPresenter provideMainActivityPresenter(TasksHelper tasksHelper, PrefHelper prefHelper) {
        return new MainActivityPresenter(tasksHelper, prefHelper);
    }

    @Provides
    EditTaskPresenter provideEditTaskPresenter(TasksHelper tasksHelper) {
        return new EditTaskPresenter(tasksHelper);
    }

    @Provides
    TaskDetailPresenter provideTaskDetailPresenter(TasksHelper tasksHelper) {
        return new TaskDetailPresenter(tasksHelper);
    }
}
