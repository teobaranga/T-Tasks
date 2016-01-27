package com.teo.ttasks.injection.module;

import android.support.annotation.NonNull;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.tasks.Tasks;
import com.teo.ttasks.data.remote.TasksHelper;
import com.teo.ttasks.injection.UserScope;

import dagger.Module;
import dagger.Provides;

@Module
public class TasksApiModule {

    private GoogleAccountCredential mGoogleAccountCredential;

    public TasksApiModule(GoogleAccountCredential credential) {
        mGoogleAccountCredential = credential;
    }

    @UserScope
    @Provides
    @NonNull
    Tasks provideTasks() {
        return new Tasks.Builder(AndroidHttp.newCompatibleTransport(), JacksonFactory.getDefaultInstance(), mGoogleAccountCredential)
                .setApplicationName("T-Tasks/0.1")
                .build();
    }

    @UserScope
    @Provides
    @NonNull
    TasksHelper provideTasksModel(@NonNull Tasks tasks) {
        return new TasksHelper(tasks);
    }
}
