package com.teo.ttasks.injection.component

import com.teo.ttasks.StartActivityForResultTest
import com.teo.ttasks.TTasksApp
import com.teo.ttasks.injection.module.InjectorsModule
import com.teo.ttasks.injection.module.TasksApiModule
import com.teo.ttasks.injection.module.TestApplicationModule
import dagger.Component
import dagger.android.AndroidInjector
import dagger.android.support.AndroidSupportInjectionModule
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        AndroidSupportInjectionModule::class,
        TestApplicationModule::class,
        TasksApiModule::class,
        InjectorsModule::class
    ]
)
interface TestApplicationComponent : ApplicationComponent {

    @Component.Builder
    abstract class Builder : AndroidInjector.Builder<TTasksApp>()

    fun inject(asd: StartActivityForResultTest)
}
