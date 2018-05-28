package com.teo.ttasks.injection.component

import com.teo.ttasks.TTasksApp
import com.teo.ttasks.injection.module.ApplicationModule
import com.teo.ttasks.injection.module.InjectorsModule
import com.teo.ttasks.injection.module.TasksApiModule
import com.teo.ttasks.jobs.DeleteTaskJob
import com.teo.ttasks.jobs.TaskCreateJob
import com.teo.ttasks.jobs.TaskUpdateJob
import dagger.Component
import dagger.android.AndroidInjector
import dagger.android.support.AndroidSupportInjectionModule
import javax.inject.Singleton

@Singleton
@Component(modules = [
    AndroidSupportInjectionModule::class,
    ApplicationModule::class,
    TasksApiModule::class,
    InjectorsModule::class
])
interface ApplicationComponent : AndroidInjector<TTasksApp> {

    @Component.Builder
    abstract class Builder : AndroidInjector.Builder<TTasksApp>() {
        abstract override fun build(): ApplicationComponent
    }

    fun inject(taskCreateJob: TaskCreateJob)

    fun inject(taskUpdateJob: TaskUpdateJob)

    fun inject(deleteTaskJob: DeleteTaskJob)
}
