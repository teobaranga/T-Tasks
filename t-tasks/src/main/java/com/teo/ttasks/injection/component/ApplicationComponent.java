package com.teo.ttasks.injection.component;

import com.teo.ttasks.TTasksApp;
import com.teo.ttasks.injection.module.ApplicationModule;
import com.teo.ttasks.injection.module.TasksModule;
import com.teo.ttasks.ui.activities.SignInActivity;
import com.teo.ttasks.ui.activities.main.MainActivity;
import com.teo.ttasks.widget.TasksRemoteViewsFactory;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = {
        ApplicationModule.class
})
public interface ApplicationComponent {

    void inject(TTasksApp tTasksApp);

    void inject(SignInActivity signInActivity);

    void inject(MainActivity mainActivity);

    void inject(TasksRemoteViewsFactory tasksRemoteViewsFactory);

    TasksComponent plus(TasksModule tasksModule);
}
