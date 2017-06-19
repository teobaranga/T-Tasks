package com.teo.ttasks.injection.module;

import android.content.Context;
import android.os.Build;

import com.birbit.android.jobqueue.JobManager;
import com.birbit.android.jobqueue.config.Configuration;
import com.birbit.android.jobqueue.scheduling.FrameworkJobSchedulerService;
import com.birbit.android.jobqueue.scheduling.GcmJobSchedulerService;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.teo.ttasks.TTasksApp;
import com.teo.ttasks.data.local.PrefHelper;
import com.teo.ttasks.data.local.WidgetHelper;
import com.teo.ttasks.jobs.CreateTaskJob;
import com.teo.ttasks.jobs.DeleteTaskJob;
import com.teo.ttasks.receivers.NetworkInfoReceiver;
import com.teo.ttasks.services.MyGcmJobService;
import com.teo.ttasks.services.MyJobService;
import com.teo.ttasks.util.NotificationHelper;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

/** It's a Dagger module that provides application level dependencies. */
@Module
public class ApplicationModule {

    public static final String SCOPE_TASKS = "https://www.googleapis.com/auth/tasks";

    @Provides Context provideContext(TTasksApp application) {
        return application.getApplicationContext();
    }

    @Provides @Singleton
    PrefHelper providePrefHelper(Context context) {
        return new PrefHelper(context);
    }

    @Provides @Singleton
    WidgetHelper provideWidgetHelper(Context context) {
        return new WidgetHelper(context);
    }

    @Provides @Singleton
    NotificationHelper provideNotificationHelper(Context context) {
        return new NotificationHelper(context);
    }

    @Provides @Singleton
    NetworkInfoReceiver provideNetworkInfoReceiver() {
        return new NetworkInfoReceiver();
    }

    @Provides @Singleton
    JobManager provideJobManager(Context context) {
        Configuration.Builder builder = new Configuration.Builder(context)
                .injector(job -> {
                    if (job instanceof CreateTaskJob)
                        TTasksApp.get(context).applicationComponent().inject(((CreateTaskJob) job));
                    else if (job instanceof DeleteTaskJob)
                        TTasksApp.get(context).applicationComponent().inject(((DeleteTaskJob) job));
                })
                .minConsumerCount(1)//always keep at least one consumer alive
                .maxConsumerCount(3)//up to 3 consumers at a time
                .loadFactor(3)//3 jobs per consumer
                .consumerKeepAlive(120);//wait 2 minute
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder.scheduler(FrameworkJobSchedulerService.createSchedulerFor(context, MyJobService.class), true);
        } else {
            int enableGcm = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context);
            if (enableGcm == ConnectionResult.SUCCESS) {
                builder.scheduler(GcmJobSchedulerService.createSchedulerFor(context, MyGcmJobService.class), true);
            }
        }
        return new JobManager(builder.build());
    }
}
