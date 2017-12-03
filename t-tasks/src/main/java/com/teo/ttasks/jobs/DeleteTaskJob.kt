package com.teo.ttasks.jobs

import com.birbit.android.jobqueue.CancelReason
import com.birbit.android.jobqueue.Job
import com.birbit.android.jobqueue.Params
import com.birbit.android.jobqueue.RetryConstraint
import com.teo.ttasks.api.TasksApi
import com.teo.ttasks.data.remote.TasksHelper
import com.teo.ttasks.delete
import com.teo.ttasks.util.FirebaseUtil
import io.realm.Realm
import timber.log.Timber
import javax.inject.Inject

class DeleteTaskJob(private val taskId: String, private val taskListId: String) :
        Job(Params(Priority.HIGH).requireNetwork().persist()) {

    @Inject @Transient internal lateinit var tasksHelper: TasksHelper
    @Inject @Transient internal lateinit var tasksApi: TasksApi

    override fun onAdded() {
        // Do nothing
    }

    @Throws(Throwable::class)
    override fun onRun() {
        // Delete the reminder
        FirebaseUtil.saveReminder(taskId, null)

        val realm = Realm.getDefaultInstance()
        val tTask = tasksHelper.getTask(taskId, realm)

        // Task not found, nothing to do here
        if (tTask == null) {
            realm.close()
            return
        }

        // Delete the Google task
        if (!tTask.isLocalOnly) {
            val response = tasksApi.deleteTask(taskListId, taskId).execute()
            response.body()

            // Handle failure
            if (!response.isSuccessful) {
                response.errorBody()?.close()
                realm.close()
                throw Exception("Failed to delete task")
            }
        }

        // Delete the Realm task
        realm.executeTransaction { tTask.delete() }
        Timber.d("deleted task %s", taskId)

        realm.close()
    }

    override fun onCancel(@CancelReason cancelReason: Int, throwable: Throwable?) {
        throwable?.let { Timber.e(it.toString()) }
    }

    override fun shouldReRunOnThrowable(throwable: Throwable, runCount: Int, maxRunCount: Int): RetryConstraint {
        return RetryConstraint.createExponentialBackoff(runCount, 1000)
    }
}
