package com.teo.ttasks.injection.component;

import android.support.annotation.NonNull;

import com.teo.ttasks.injection.UserScope;
import com.teo.ttasks.injection.module.TasksModule;
import com.teo.ttasks.ui.activities.main.MainActivity;
import com.teo.ttasks.ui.fragments.tasks.TasksFragment;

import dagger.Subcomponent;

@UserScope
@Subcomponent(modules = TasksModule.class)
public interface TasksComponent {
    void inject(@NonNull MainActivity mainActivity);

    void inject(@NonNull TasksFragment tasksFragment);
}
