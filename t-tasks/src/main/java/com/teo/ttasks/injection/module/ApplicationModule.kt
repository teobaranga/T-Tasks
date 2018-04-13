package com.teo.ttasks.injection.module

import android.content.Context
import com.birbit.android.jobqueue.JobManager
import com.birbit.android.jobqueue.config.Configuration
import com.birbit.android.jobqueue.scheduling.FrameworkJobSchedulerService
import com.teo.ttasks.TTasksApp
import com.teo.ttasks.data.local.PrefHelper
import com.teo.ttasks.data.local.WidgetHelper
import com.teo.ttasks.jobs.DeleteTaskJob
import com.teo.ttasks.receivers.NetworkInfoReceiver
import com.teo.ttasks.services.MyJobService
import com.teo.ttasks.util.NotificationHelper
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

/** It's a Dagger module that provides application level dependencies.  */
@Module
class ApplicationModule {

    companion object {
        const val SCOPE_TASKS = "https://www.googleapis.com/auth/tasks"
    }

    @Provides
    internal fun provideContext(application: TTasksApp): Context {
        return application.applicationContext
    }

    @Provides
    @Singleton
    internal fun providePrefHelper(context: Context): PrefHelper {
        return PrefHelper(context)
    }

    @Provides
    @Singleton
    internal fun provideWidgetHelper(context: Context, prefHelper: PrefHelper): WidgetHelper {
        return WidgetHelper(context, prefHelper)
    }

    @Provides
    @Singleton
    internal fun provideNotificationHelper(context: Context): NotificationHelper {
        return NotificationHelper(context)
    }

    @Provides
    @Singleton
    internal fun provideNetworkInfoReceiver(): NetworkInfoReceiver {
        return NetworkInfoReceiver()
    }

    @Provides
    @Singleton
    internal fun provideJobManager(application: TTasksApp): JobManager {
        val builder = Configuration.Builder(application)
                .injector { job ->
                    if (job is DeleteTaskJob)
                        application.applicationComponent().inject(job)
                }
                .minConsumerCount(1)//always keep at least one consumer alive
                .maxConsumerCount(3)//up to 3 consumers at a time
                .loadFactor(3)//3 jobs per consumer
                .consumerKeepAlive(120)//wait 2 minute
        builder.scheduler(FrameworkJobSchedulerService.createSchedulerFor(application, MyJobService::class.java), true)
        return JobManager(builder.build())
    }
}
