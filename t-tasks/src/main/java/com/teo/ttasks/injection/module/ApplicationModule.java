package com.teo.ttasks.injection.module;

import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;

import com.teo.ttasks.TTasksApp;

import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import rx.Scheduler;
import rx.android.schedulers.HandlerScheduler;

/** It's a Dagger module that provides application level dependencies. */
@Module
public class ApplicationModule {

    public static final String REALM_SCHEDULER = "realm_scheduler";

    @NonNull
    private final TTasksApp ttasksApp;

    public ApplicationModule(@NonNull TTasksApp ttasksApp) {
        this.ttasksApp = ttasksApp;
    }

    @Provides
    @NonNull
    @Singleton
    public TTasksApp provideTTasksApp() {
        return ttasksApp;
    }

    @Provides
    @NonNull
    @Named(REALM_SCHEDULER)
    @Singleton
    public Scheduler provideRealmScheduler() {
        HandlerThread handlerThread = new HandlerThread("Realm");
        handlerThread.start();
        return HandlerScheduler.from(new Handler(handlerThread.getLooper()));
    }

}
