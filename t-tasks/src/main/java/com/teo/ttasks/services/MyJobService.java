package com.teo.ttasks.services;

import android.support.annotation.NonNull;

import com.birbit.android.jobqueue.JobManager;
import com.birbit.android.jobqueue.scheduling.FrameworkJobSchedulerService;
import com.teo.ttasks.TTasksApp;

import javax.inject.Inject;

public class MyJobService extends FrameworkJobSchedulerService {

    @Inject JobManager jobManager;

    @Override @NonNull
    protected JobManager getJobManager() {
        if (jobManager == null)
            TTasksApp.get(getApplicationContext()).userComponent().inject(this);
        return jobManager;
    }
}
