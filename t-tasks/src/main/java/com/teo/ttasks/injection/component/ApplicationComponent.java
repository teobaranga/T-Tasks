package com.teo.ttasks.injection.component;

import com.teo.ttasks.TTasksApp;
import com.teo.ttasks.injection.module.ApplicationModule;
import com.teo.ttasks.injection.module.SignInModule;
import com.teo.ttasks.injection.module.TasksApiModule;
import com.teo.ttasks.injection.module.UserModule;
import com.teo.ttasks.receivers.TaskNotificationReceiver;
import com.teo.ttasks.widget.TasksRemoteViewsFactory;
import com.teo.ttasks.widget.TasksWidgetProvider;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = {
        ApplicationModule.class,
        TasksApiModule.class
})
public interface ApplicationComponent {

    void inject(TTasksApp tTasksApp);

    void inject(TasksRemoteViewsFactory tasksRemoteViewsFactory);

    void inject(TasksWidgetProvider tasksWidgetProvider);

    void inject(TaskNotificationReceiver taskNotificationReceiver);

    SignInComponent plus(SignInModule signInModule);

    UserComponent plus(UserModule userModule);
}
