package com.teo.ttasks.injection.module;

import com.teo.ttasks.injection.module.activity.EditTaskActivityModule;
import com.teo.ttasks.injection.module.activity.MainActivityModule;
import com.teo.ttasks.injection.module.activity.SignInActivityModule;
import com.teo.ttasks.injection.module.activity.TaskDetailActivityModule;
import com.teo.ttasks.injection.module.activity.TasksWidgetConfigureActivityModule;
import com.teo.ttasks.receivers.TaskNotificationReceiver;
import com.teo.ttasks.services.MyGcmJobService;
import com.teo.ttasks.services.MyJobService;
import com.teo.ttasks.ui.activities.edit_task.EditTaskActivity;
import com.teo.ttasks.ui.activities.main.MainActivity;
import com.teo.ttasks.ui.activities.sign_in.SignInActivity;
import com.teo.ttasks.ui.activities.task_detail.TaskDetailActivity;
import com.teo.ttasks.widget.TasksWidgetProvider;
import com.teo.ttasks.widget.TasksWidgetService;
import com.teo.ttasks.widget.configure.TasksWidgetConfigureActivity;

import dagger.Module;
import dagger.android.ContributesAndroidInjector;

@Module
public abstract class InjectorsModule {

    /********************************
     * Activities
     ********************************/

    @ContributesAndroidInjector(modules = {SignInActivityModule.class})
    abstract SignInActivity contributeSignInActivityInjector();

    @ContributesAndroidInjector(modules = {MainActivityModule.class})
    abstract MainActivity contributeMainActivityInjector();

    @ContributesAndroidInjector(modules = {TaskDetailActivityModule.class})
    abstract TaskDetailActivity contributeTaskDetailActivityInjector();

    @ContributesAndroidInjector(modules = {EditTaskActivityModule.class})
    abstract EditTaskActivity contributeEditTaskActivityInjector();

    @ContributesAndroidInjector(modules = {TasksWidgetConfigureActivityModule.class})
    abstract TasksWidgetConfigureActivity contributeTasksWidgetConfigureActivityInjector();

    /********************************
     * Services
     ********************************/

    @ContributesAndroidInjector
    abstract TasksWidgetService contributeTasksWidgetServiceInjector();

    @ContributesAndroidInjector
    abstract TasksWidgetProvider contributeTasksWidgetProviderInjector();

    @ContributesAndroidInjector
    abstract MyJobService contributeMyJobServiceInjector();

    @ContributesAndroidInjector(modules = {TasksWidgetConfigureActivityModule.class})
    abstract MyGcmJobService contributeMyGcmJobServiceInjector();

    /********************************
     * BroadcastReceivers
     ********************************/

    @ContributesAndroidInjector
    abstract TaskNotificationReceiver contributeTaskNotificationReceiverInjector();
}
