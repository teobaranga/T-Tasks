package com.teo.ttasks.jobs

import com.evernote.android.job.Job
import com.evernote.android.job.JobManager
import com.evernote.android.job.JobRequest
import com.evernote.android.job.util.support.PersistableBundleCompat
import com.google.firebase.database.FirebaseDatabase
import com.teo.ttasks.TTasksApp
import com.teo.ttasks.api.TasksApi
import com.teo.ttasks.data.remote.TasksHelper
import com.teo.ttasks.delete
import com.teo.ttasks.util.FirebaseUtil.getTasksDatabase
import com.teo.ttasks.util.FirebaseUtil.saveReminder
import io.realm.Realm
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class DeleteTaskJob : Job() {

    companion object {
        const val TAG = "DELETE_TASK"
        const val EXTRA_TASK_ID = "taskId"
        const val EXTRA_TASK_LIST_ID = "taskListId"

        fun schedule(taskId: String, taskListId: String) {
            JobManager.instance().getAllJobRequestsForTag(TAG)
                    .forEach {
                        if (it.extras[EXTRA_TASK_ID] as String == taskId &&
                                it.extras[EXTRA_TASK_LIST_ID] as String == taskListId) {
                            Timber.v("Delete Task job already exists for %s, ignoring...", taskId)
                            return
                        }
                    }
            val extras = PersistableBundleCompat().apply {
                putString(EXTRA_TASK_ID, taskId)
                putString(EXTRA_TASK_LIST_ID, taskListId)
            }
            JobRequest.Builder(TAG)
                    .setBackoffCriteria(5_000L, JobRequest.BackoffPolicy.EXPONENTIAL)
                    .setRequiredNetworkType(JobRequest.NetworkType.CONNECTED)
                    .setExtras(extras)
                    .setRequirementsEnforced(true)
                    .setUpdateCurrent(true)
                    .setExecutionWindow(1, TimeUnit.DAYS.toMillis(1))
                    .build()
                    .schedule()
        }
    }

    @Inject @Transient internal lateinit var tasksHelper: TasksHelper
    @Inject @Transient internal lateinit var tasksApi: TasksApi

    override fun onRunJob(params: Params): Result {
        // Inject dependencies
        (context.applicationContext as TTasksApp).applicationComponent().inject(this)

        // Extract params
        val extras = params.extras
        val taskId = extras[EXTRA_TASK_ID] as String
        val taskListId = extras[EXTRA_TASK_LIST_ID] as String

        // Delete the reminder
        val tasksDatabase = FirebaseDatabase.getInstance().getTasksDatabase()
        tasksDatabase.saveReminder(taskId, null)

        val realm = Realm.getDefaultInstance()
        val tTask = tasksHelper.getTask(taskId, realm)

        // Task not found, nothing to do here
        if (tTask == null) {
            realm.close()
            return Job.Result.SUCCESS
        }

        // Delete the Google task
        if (!tTask.isLocalOnly) {
            val result = tasksApi.deleteTask(taskListId, taskId).blockingGet()
            if (result != null) {
                Timber.e(result, "Error while deleting remote task")
                realm.close()
                return Job.Result.RESCHEDULE
            }
        }

        // Delete the Realm task
        realm.executeTransaction { tTask.delete() }
        Timber.v("Deleted task %s", taskId)

        realm.close()
        return Job.Result.SUCCESS
    }

    override fun onCancel() {
        Timber.e("Delete Task job cancelled")
//        throwable?.let { Timber.e(it.toString()) }
    }
}
