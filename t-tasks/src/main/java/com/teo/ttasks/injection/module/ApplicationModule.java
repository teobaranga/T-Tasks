package com.teo.ttasks.injection.module;

import com.teo.ttasks.TTasksApp;
import com.teo.ttasks.data.local.PrefHelper;
import com.teo.ttasks.data.local.WidgetHelper;
import com.teo.ttasks.receivers.NetworkInfoReceiver;
import com.teo.ttasks.util.NotificationHelper;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

/** It's a Dagger module that provides application level dependencies. */
@Module
public class ApplicationModule {

    public static final String SCOPE_TASKS = "https://www.googleapis.com/auth/tasks";

    private final TTasksApp ttasksApp;

    public ApplicationModule(TTasksApp ttasksApp) {
        this.ttasksApp = ttasksApp;
    }

    @Provides @Singleton
    TTasksApp provideTTasksApp() {
        return ttasksApp;
    }

    @Provides @Singleton
    PrefHelper providePrefHelper() {
        return new PrefHelper(ttasksApp);
    }

    @Provides @Singleton
    WidgetHelper provideWidgetHelper() {
        return new WidgetHelper(ttasksApp);
    }

    @Provides @Singleton
    NotificationHelper provideNotificationHelper() {
        return new NotificationHelper(ttasksApp);
    }

    @Provides @Singleton
    NetworkInfoReceiver provideNetworkInfoReceiver() {
        return new NetworkInfoReceiver();
    }
}
