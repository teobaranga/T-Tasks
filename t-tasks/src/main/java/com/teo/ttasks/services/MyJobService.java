package com.teo.ttasks.services;

import android.support.annotation.NonNull;

import com.birbit.android.jobqueue.JobManager;
import com.birbit.android.jobqueue.scheduling.FrameworkJobSchedulerService;

import javax.inject.Inject;

import dagger.android.AndroidInjection;

public class MyJobService extends FrameworkJobSchedulerService {

    @Inject JobManager jobManager;

    @Override @NonNull
    protected JobManager getJobManager() {
        if (jobManager == null)
            AndroidInjection.inject(this);
        return jobManager;
    }
}
