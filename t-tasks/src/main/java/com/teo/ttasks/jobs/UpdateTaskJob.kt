package com.teo.ttasks.jobs

import com.birbit.android.jobqueue.Job
import com.birbit.android.jobqueue.Params
import com.birbit.android.jobqueue.RetryConstraint

class UpdateTaskJob(params: Params) : Job(params) {

    override fun onAdded() {

    }

    @Throws(Throwable::class)
    override fun onRun() {

    }

    override fun onCancel(cancelReason: Int, throwable: Throwable?) {

    }

    override fun shouldReRunOnThrowable(throwable: Throwable, runCount: Int, maxRunCount: Int): RetryConstraint? {
        return null
    }
}
