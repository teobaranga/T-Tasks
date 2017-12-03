package com.teo.ttasks.services

import com.birbit.android.jobqueue.JobManager
import com.birbit.android.jobqueue.scheduling.GcmJobSchedulerService
import dagger.android.AndroidInjection
import javax.inject.Inject

class MyGcmJobService : GcmJobSchedulerService() {

    @Inject internal lateinit var jobManager: JobManager

    override fun getJobManager(): JobManager {
        AndroidInjection.inject(this)
        return jobManager
    }
}
