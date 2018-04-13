package com.teo.ttasks.jobs

import com.evernote.android.job.Job
import com.evernote.android.job.JobCreator

class DefaultJobCreator : JobCreator {

    override fun create(tag: String): Job? {
        return when (tag) {
            CreateTaskJob.TAG -> CreateTaskJob()
            else -> null
        }
    }
}
