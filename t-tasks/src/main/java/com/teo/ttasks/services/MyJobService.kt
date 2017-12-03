package com.teo.ttasks.services

import com.birbit.android.jobqueue.JobManager
import com.birbit.android.jobqueue.scheduling.FrameworkJobSchedulerService
import dagger.android.AndroidInjection
import javax.inject.Inject

class MyJobService : FrameworkJobSchedulerService() {

    @Inject internal lateinit var jobManager: JobManager

    override fun getJobManager(): JobManager {
        AndroidInjection.inject(this)
        return jobManager
    }
}
