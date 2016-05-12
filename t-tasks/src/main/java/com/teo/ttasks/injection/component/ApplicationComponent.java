package com.teo.ttasks.injection.component;

import android.support.annotation.NonNull;

import com.teo.ttasks.TTasksApp;
import com.teo.ttasks.injection.module.ApplicationModule;
import com.teo.ttasks.injection.module.TasksModule;
import com.teo.ttasks.widget.TasksRemoteViewsFactory;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = {
        ApplicationModule.class
})
public interface ApplicationComponent {

    void inject(@NonNull TTasksApp tTasksApp);

    void inject(@NonNull TasksRemoteViewsFactory tasksRemoteViewsFactory);

    TasksComponent plus(TasksModule tasksModule);

}
