package com.teo.ttasks.performance;

import android.support.annotation.NonNull;

import com.teo.ttasks.injection.AnyThread;

public interface AsyncJobsObserver {

    interface Listener {
        @AnyThread
        void onNumberOfRunningAsyncJobsChanged(int numberOfRunningAsyncJobs);
    }

    @AnyThread
    void addListener(@NonNull Listener listener);

    @AnyThread
    void removeListener(@NonNull Listener listener);

    @AnyThread
    int numberOfRunningAsyncJobs();

    @AnyThread
    @NonNull
    AsyncJob asyncJobStarted(@NonNull String name);

    @AnyThread
    void asyncJobFinished(@NonNull AsyncJob asyncJob);
}
