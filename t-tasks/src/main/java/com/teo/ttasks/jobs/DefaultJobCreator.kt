package com.teo.ttasks.jobs

import com.evernote.android.job.Job
import com.evernote.android.job.JobCreator
import org.koin.core.KoinComponent
import org.koin.core.inject

class DefaultJobCreator : JobCreator, KoinComponent {

    override fun create(tag: String): Job? {
        return when (tag) {
            TaskCreateJob.TAG -> inject<TaskCreateJob>().value
            TaskUpdateJob.TAG -> inject<TaskUpdateJob>().value
            TaskDeleteJob.TAG -> inject<TaskDeleteJob>().value
            else -> null
        }
    }
}
