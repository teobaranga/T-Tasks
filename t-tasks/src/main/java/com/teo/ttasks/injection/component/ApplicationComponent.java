package com.teo.ttasks.injection.component;

import android.support.annotation.NonNull;

import com.teo.ttasks.TTasksApp;
import com.teo.ttasks.injection.module.ApplicationModule;
import com.teo.ttasks.injection.module.TasksModule;
import com.teo.ttasks.performance.AsyncJobsModule;
import com.teo.ttasks.performance.AsyncJobsObserver;
import com.teo.ttasks.ui.fragments.BaseFragment;
import com.teo.ttasks.widget.TasksRemoteViewsFactory;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = {
        ApplicationModule.class,
        AsyncJobsModule.class,
})
public interface ApplicationComponent {

    // Provide AsyncJobObserver from the real app to the tests without need in injection to the test.
    @NonNull
    AsyncJobsObserver asyncJobsObserver();

    void inject(@NonNull TTasksApp tTasksApp);

    void inject(@NonNull BaseFragment baseFragment);

    void inject(@NonNull TasksRemoteViewsFactory tasksRemoteViewsFactory);

    TasksComponent plus(TasksModule tasksModule);

}
