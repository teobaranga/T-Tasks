package com.teo.ttasks.injection.module

import com.teo.ttasks.injection.module.activity.*
import com.teo.ttasks.injection.module.fragment.MainFragmentsModule
import com.teo.ttasks.receivers.TaskNotificationReceiver
import com.teo.ttasks.services.MyGcmJobService
import com.teo.ttasks.services.MyJobService
import com.teo.ttasks.ui.activities.edit_task.EditTaskActivity
import com.teo.ttasks.ui.activities.main.MainActivity
import com.teo.ttasks.ui.activities.sign_in.SignInActivity
import com.teo.ttasks.ui.activities.task_detail.TaskDetailActivity
import com.teo.ttasks.widget.TasksWidgetProvider
import com.teo.ttasks.widget.TasksWidgetService
import com.teo.ttasks.widget.configure.TasksWidgetConfigureActivity
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
abstract class InjectorsModule {

    /********************************
     * Activities
     ********************************/

    @ContributesAndroidInjector(modules = arrayOf(SignInActivityModule::class))
    internal abstract fun contributeSignInActivityInjector(): SignInActivity

    @ContributesAndroidInjector(modules = arrayOf(MainActivityModule::class, MainFragmentsModule::class))
    internal abstract fun contributeMainActivityInjector(): MainActivity

    @ContributesAndroidInjector(modules = arrayOf(TaskDetailActivityModule::class))
    internal abstract fun contributeTaskDetailActivityInjector(): TaskDetailActivity

    @ContributesAndroidInjector(modules = arrayOf(EditTaskActivityModule::class))
    internal abstract fun contributeEditTaskActivityInjector(): EditTaskActivity

    @ContributesAndroidInjector(modules = arrayOf(TasksWidgetConfigureActivityModule::class))
    internal abstract fun contributeTasksWidgetConfigureActivityInjector(): TasksWidgetConfigureActivity

    /********************************
     * Services
     ********************************/

    @ContributesAndroidInjector
    internal abstract fun contributeTasksWidgetServiceInjector(): TasksWidgetService

    @ContributesAndroidInjector
    internal abstract fun contributeTasksWidgetProviderInjector(): TasksWidgetProvider

    @ContributesAndroidInjector
    internal abstract fun contributeMyJobServiceInjector(): MyJobService

    @ContributesAndroidInjector(modules = arrayOf(TasksWidgetConfigureActivityModule::class))
    internal abstract fun contributeMyGcmJobServiceInjector(): MyGcmJobService

    /********************************
     * BroadcastReceivers
     ********************************/

    @ContributesAndroidInjector
    internal abstract fun contributeTaskNotificationReceiverInjector(): TaskNotificationReceiver
}
