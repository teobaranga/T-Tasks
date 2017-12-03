package com.teo.ttasks.injection.module

import android.content.Context
import android.os.Build
import com.birbit.android.jobqueue.JobManager
import com.birbit.android.jobqueue.config.Configuration
import com.birbit.android.jobqueue.scheduling.FrameworkJobSchedulerService
import com.birbit.android.jobqueue.scheduling.GcmJobSchedulerService
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.teo.ttasks.TTasksApp
import com.teo.ttasks.data.local.PrefHelper
import com.teo.ttasks.data.local.WidgetHelper
import com.teo.ttasks.jobs.CreateTaskJob
import com.teo.ttasks.jobs.DeleteTaskJob
import com.teo.ttasks.receivers.NetworkInfoReceiver
import com.teo.ttasks.services.MyGcmJobService
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
    internal fun provideJobManager(context: Context): JobManager {
        val builder = Configuration.Builder(context)
                .injector { job ->
                    if (job is CreateTaskJob)
                        TTasksApp[context].applicationComponent().inject(job)
                    else if (job is DeleteTaskJob)
                        TTasksApp[context].applicationComponent().inject(job)
                }
                .minConsumerCount(1)//always keep at least one consumer alive
                .maxConsumerCount(3)//up to 3 consumers at a time
                .loadFactor(3)//3 jobs per consumer
                .consumerKeepAlive(120)//wait 2 minute
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder.scheduler(FrameworkJobSchedulerService.createSchedulerFor(context, MyJobService::class.java), true)
        } else {
            val enableGcm = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context)
            if (enableGcm == ConnectionResult.SUCCESS) {
                builder.scheduler(GcmJobSchedulerService.createSchedulerFor(context, MyGcmJobService::class.java), true)
            }
        }
        return JobManager(builder.build())
    }
}
