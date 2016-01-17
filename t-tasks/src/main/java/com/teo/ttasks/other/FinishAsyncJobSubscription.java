package com.teo.ttasks.other;

import android.support.annotation.NonNull;

import com.teo.ttasks.performance.AsyncJob;
import com.teo.ttasks.performance.AsyncJobsObserver;

public class FinishAsyncJobSubscription extends DisposableSubscription {

    @SuppressWarnings("PMD.EmptyCatchBlock")
    public FinishAsyncJobSubscription(@NonNull AsyncJobsObserver asyncJobsObserver, @NonNull AsyncJob asyncJob) {
        super(() -> {
            try {
                asyncJobsObserver.asyncJobFinished(asyncJob);
            } catch (IllegalArgumentException possible) {
                // Do nothing, async job was probably already finished normally.
            }
        });
    }
}