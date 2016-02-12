package com.teo.ttasks.injection.module;

import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;

import com.teo.ttasks.TTasksApp;

import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import io.realm.Realm;

/** It's a Dagger module that provides application level dependencies. */
@Module
public class ApplicationModule {

    public static final String MAIN_THREAD_HANDLER = "main_thread_handler";

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
    @Named(MAIN_THREAD_HANDLER)
    @Singleton
    public Handler provideMainThreadHandler() {
        return new Handler(Looper.getMainLooper());
    }

    @Provides
    @NonNull
    public Realm provideRealm() {
        return Realm.getDefaultInstance();
    }

}
