package com.teo.ttasks.jobs

import com.evernote.android.job.Job
import com.evernote.android.job.JobManager
import com.evernote.android.job.JobRequest
import com.evernote.android.job.util.support.PersistableBundleCompat
import com.teo.ttasks.TTasksApp
import com.teo.ttasks.api.TasksApi
import com.teo.ttasks.data.local.TaskFields
import com.teo.ttasks.data.local.WidgetHelper
import com.teo.ttasks.data.model.Task
import com.teo.ttasks.data.remote.TasksHelper
import com.teo.ttasks.util.NotificationHelper
import io.realm.Realm
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class TaskUpdateJob : Job() {

    companion object {
        const val TAG = "UPDATE_TASK"
        const val EXTRA_LOCAL_ID = "localId"
        const val EXTRA_TASK_LIST_ID = "taskListId"

        fun schedule(localTaskId: String, taskListId: String, taskFields: TaskFields? = null) {
            val extras = PersistableBundleCompat().apply {
                putString(EXTRA_LOCAL_ID, localTaskId)
                putString(EXTRA_TASK_LIST_ID, taskListId)
                taskFields?.forEach {
                    putString(it.key, it.value)
                }
            }
            JobManager.instance().getAllJobRequestsForTag(TAG)
                    .forEach {
                        if (it.extras[EXTRA_LOCAL_ID] as String == localTaskId) {
                            Timber.v("Update Task job already exists for %s, rescheduling...", localTaskId)
                            it.cancelAndEdit()
                                    .setExtras(extras)
                                    .setExecutionWindow(1, TimeUnit.DAYS.toMillis(7))
                                    .build()
                                    .schedule()
                            return
                        }
                    }
            JobRequest.Builder(TAG)
                    .setBackoffCriteria(5_000L, JobRequest.BackoffPolicy.EXPONENTIAL)
                    .setRequiredNetworkType(JobRequest.NetworkType.CONNECTED)
                    .setExtras(extras)
                    .setRequirementsEnforced(true)
                    .setUpdateCurrent(true)
                    // Attempt this job for a week
                    .setExecutionWindow(1, TimeUnit.DAYS.toMillis(7))
                    .build()
                    .schedule()
        }

        fun cancel(taskId: String, taskListId: String) {
            val jobManager = JobManager.instance()
            jobManager.getAllJobRequestsForTag(TAG)
                    .forEach {
                        if (it.extras[EXTRA_LOCAL_ID] as String == taskId &&
                                it.extras[EXTRA_TASK_LIST_ID] as String == taskListId) {
                            jobManager.cancel(it.jobId)
                            Timber.v("Update Task job cancelled for %s", taskId)
                            return
                        }
                    }
        }
    }

    @Inject @Transient internal lateinit var tasksHelper: TasksHelper
    @Inject @Transient internal lateinit var widgetHelper: WidgetHelper
    @Inject @Transient internal lateinit var notificationHelper: NotificationHelper
    @Inject @Transient internal lateinit var tasksApi: TasksApi

    override fun onRunJob(params: Params): Result {
        Timber.v("Task Update Job running...")

        if (params.failureCount >= 10) {
            Timber.w("Task Update Job failed 10 times, abandoning")
            return Job.Result.FAILURE
        }

        // Inject dependencies
        (context.applicationContext as TTasksApp).applicationComponent.inject(this)

        // Extract params
        val extras = params.extras
        val localTaskId = extras[EXTRA_LOCAL_ID] as String
        val taskListId = extras[EXTRA_TASK_LIST_ID] as String
        val taskFields = TaskFields.fromBundle(extras)

        val realm = Realm.getDefaultInstance()
        val localTask = tasksHelper.getTask(localTaskId, realm)

        // Local task was not found, it was probably deleted, no point in continuing
        if (localTask == null) {
            Timber.v("Task not found - Success")
            realm.close()
            return Job.Result.SUCCESS
        }

        // The task was updated elsewhere
        if (localTask.synced) {
            Timber.v("Task was already synced - Success")
            realm.close()
            return Result.SUCCESS
        }

        val onlineTask: Task
        try {
            onlineTask =
                    if (taskFields != null) {
                        tasksApi.updateTask(localTaskId, taskListId, taskFields).blockingGet()
                    } else {
                        tasksApi.updateTask(localTaskId, taskListId, localTask).blockingGet()
                    }
        } catch (ex: Exception) {
            // Handle failure
            Timber.e("Failed to update the task, will retry...")
            realm.close()
            return Job.Result.RESCHEDULE
        }

        // Mark the task as synced
        onlineTask.synced = true
        // Update the local task with the full information and delete the old task
        realm.executeTransaction {
            it.insertOrUpdate(onlineTask)
        }
        realm.close()

        // Update the widget
        widgetHelper.updateWidgets(taskListId)

        // Update the previous notification with the correct task ID
        // as long the notification hasn't been dismissed
        if (!onlineTask.notificationDismissed) {
            notificationHelper.scheduleTaskNotification(onlineTask, onlineTask.notificationId)
        }

        Timber.d("Update Task job success - %s", localTaskId)
        return Job.Result.SUCCESS
    }

    override fun onCancel() {
        Timber.e("Update Task job cancelled")
//        throwable?.let { Timber.e(it.toString()) }
    }
}
