package com.teo.ttasks.injection.component;

import com.teo.ttasks.TTasksApp;
import com.teo.ttasks.injection.module.ApplicationModule;
import com.teo.ttasks.injection.module.InjectorsModule;
import com.teo.ttasks.injection.module.TasksApiModule;
import com.teo.ttasks.jobs.CreateTaskJob;
import com.teo.ttasks.jobs.DeleteTaskJob;

import javax.inject.Singleton;

import dagger.Component;
import dagger.android.AndroidInjector;
import dagger.android.support.AndroidSupportInjectionModule;

@Singleton
@Component(modules = {
        AndroidSupportInjectionModule.class,
        ApplicationModule.class,
        TasksApiModule.class,

        InjectorsModule.class
})
public interface ApplicationComponent extends AndroidInjector<TTasksApp> {

    @Component.Builder
    abstract class Builder extends AndroidInjector.Builder<TTasksApp> {
        public abstract ApplicationComponent build();
    }

    void inject(CreateTaskJob createTaskJob);

    void inject(DeleteTaskJob deleteTaskJob);
}
