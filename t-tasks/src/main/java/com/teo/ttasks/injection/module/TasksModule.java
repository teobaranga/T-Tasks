package com.teo.ttasks.injection.module;

import android.support.annotation.NonNull;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.extensions.android.json.AndroidJsonFactory;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.tasks.Tasks;
import com.google.api.services.tasks.TasksScopes;
import com.teo.ttasks.TTasksApp;
import com.teo.ttasks.data.local.PrefHelper;
import com.teo.ttasks.data.local.RealmHelper;
import com.teo.ttasks.data.remote.TasksHelper;
import com.teo.ttasks.injection.UserScope;
import com.teo.ttasks.ui.activities.main.MainActivityPresenter;
import com.teo.ttasks.ui.fragments.tasks.TasksPresenter;

import java.util.Collections;

import javax.inject.Named;

import dagger.Module;
import dagger.Provides;
import rx.Scheduler;

import static com.teo.ttasks.injection.module.ApplicationModule.REALM_SCHEDULER;

@Module
public class TasksModule {
    @UserScope
    @Provides
    @NonNull
    public Tasks provideTasks(@NonNull TTasksApp tTasksApp) {
        // This will fail if there is no current user
        GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(tTasksApp, Collections.singleton(TasksScopes.TASKS))
                .setBackOff(new ExponentialBackOff())
                .setSelectedAccountName(PrefHelper.getUserEmail(tTasksApp));
        return new Tasks.Builder(AndroidHttp.newCompatibleTransport(), AndroidJsonFactory.getDefaultInstance(), credential)
                .setApplicationName("T-Tasks/0.1")
                .build();
    }

    @UserScope
    @Provides
    @NonNull
    public TasksHelper provideTasksHelper(@NonNull Tasks tasks) {
        return new TasksHelper(tasks);
    }

    @UserScope
    @Provides
    @NonNull
    public MainActivityPresenter provideMainActivityPresenter(@NonNull TasksHelper tasksHelper,
                                                              @NonNull RealmHelper realmHelper,
                                                              @NonNull @Named(REALM_SCHEDULER) Scheduler realmScheduler) {
        return new MainActivityPresenter(tasksHelper, realmHelper, realmScheduler);
    }

    @UserScope
    @Provides
    @NonNull
    public TasksPresenter provideTasksPresenter(@NonNull TasksHelper tasksHelper,
                                                @NonNull RealmHelper realmHelper,
                                                @NonNull @Named(REALM_SCHEDULER) Scheduler realmScheduler) {
        return new TasksPresenter(tasksHelper, realmHelper, realmScheduler);
    }
}
