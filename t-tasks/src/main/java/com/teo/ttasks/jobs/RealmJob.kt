package com.teo.ttasks.jobs

import com.evernote.android.job.Job
import io.realm.Realm

abstract class RealmJob: Job() {

    override fun onRunJob(params: Params): Result {
        val realm = Realm.getDefaultInstance()
        val result = onRunJob(params, realm)
        realm.close()
        return result
    }

    abstract fun onRunJob(params: Params, realm: Realm): Result
}
