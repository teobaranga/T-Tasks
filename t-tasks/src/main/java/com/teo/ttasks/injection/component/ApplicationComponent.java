package com.teo.ttasks.injection.component;

import com.teo.ttasks.TTasksApp;
import com.teo.ttasks.injection.module.ApplicationModule;
import com.teo.ttasks.injection.module.SignInModule;
import com.teo.ttasks.injection.module.TasksApiModule;
import com.teo.ttasks.injection.module.UserModule;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = {
        ApplicationModule.class,
        TasksApiModule.class
})
public interface ApplicationComponent {

    void inject(TTasksApp tTasksApp);

    SignInComponent plus(SignInModule signInModule);

    UserComponent plus(UserModule userModule);
}
