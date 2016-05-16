package com.teo.ttasks.injection.module;

import android.os.HandlerThread;
import android.support.annotation.NonNull;

import com.teo.ttasks.TTasksApp;
import com.teo.ttasks.data.local.PrefHelper;
import com.teo.ttasks.data.local.RealmHelper;
import com.teo.ttasks.ui.activities.main.MainActivityPresenter;

import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import rx.Scheduler;
import rx.android.schedulers.AndroidSchedulers;

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
    @Singleton
    public PrefHelper providePrefHelper() {
        return new PrefHelper(ttasksApp);
    }

    @Provides
    @NonNull
    @Named(REALM_SCHEDULER)
    @Singleton
    public Scheduler provideRealmScheduler() {
        HandlerThread handlerThread = new HandlerThread("Realm");
        handlerThread.start();
        return AndroidSchedulers.from(handlerThread.getLooper());
    }

    @Provides
    @NonNull
    public MainActivityPresenter provideMainActivityPresenter(@NonNull RealmHelper realmHelper,
                                                              @NonNull @Named(REALM_SCHEDULER) Scheduler realmScheduler) {
        return new MainActivityPresenter(realmHelper, realmScheduler);
    }

}
