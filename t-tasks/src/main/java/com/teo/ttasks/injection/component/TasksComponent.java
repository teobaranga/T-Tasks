package com.teo.ttasks.injection.component;

import com.teo.ttasks.injection.UserScope;
import com.teo.ttasks.injection.module.TasksModule;
import com.teo.ttasks.ui.activities.main.MainActivityPresenter;
import com.teo.ttasks.ui.fragments.tasks.TasksFragment;

import dagger.Subcomponent;

@UserScope
@Subcomponent(modules = TasksModule.class)
public interface TasksComponent {
    void inject(MainActivityPresenter mainActivityPresenter);

    void inject(TasksFragment tasksFragment);
}
