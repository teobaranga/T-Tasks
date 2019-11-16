package com.teo.ttasks.jobs

import com.evernote.android.job.JobManager
import com.evernote.android.job.JobRequest
import com.evernote.android.job.util.support.PersistableBundleCompat
import com.google.firebase.database.FirebaseDatabase
import com.teo.ttasks.api.TasksApi
import com.teo.ttasks.data.local.TaskFields
import com.teo.ttasks.data.local.WidgetHelper
import com.teo.ttasks.data.model.Task
import com.teo.ttasks.data.remote.TasksHelper
import com.teo.ttasks.util.FirebaseUtil.getTasksDatabase
import com.teo.ttasks.util.FirebaseUtil.saveReminder
import com.teo.ttasks.util.NotificationHelper
import io.realm.Realm
import timber.log.Timber
import java.util.concurrent.TimeUnit

class TaskCreateJob(
    @Transient private val tasksHelper: TasksHelper,
    @Transient private val widgetHelper: WidgetHelper,
    @Transient private val notificationHelper: NotificationHelper,
    @Transient private val tasksApi: TasksApi
) : RealmJob() {

    companion object {
        const val TAG = "CREATE_TASK"
        const val EXTRA_LOCAL_ID = "localId"
        const val EXTRA_TASK_LIST_ID = "taskListId"

        fun schedule(localTaskId: String, taskListId: String, taskFields: TaskFields? = null) {
            JobManager.instance().getAllJobRequestsForTag(TAG)
                .forEach {
                    if (it.extras[EXTRA_LOCAL_ID] as String == localTaskId) {
                        Timber.v("Create Task job already exists for %s, ignoring...", localTaskId)
                        return
                    }
                }
            val extras = PersistableBundleCompat().apply {
                putString(EXTRA_LOCAL_ID, localTaskId)
                putString(EXTRA_TASK_LIST_ID, taskListId)
                taskFields?.forEach {
                    putString(it.key, it.value)
                }
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

    override fun onRunJob(params: Params, realm: Realm): Result {
        // Extract params
        val extras = params.extras
        val localTaskId = extras[EXTRA_LOCAL_ID] as String
        val taskListId = extras[EXTRA_TASK_LIST_ID] as String
        val taskFields = TaskFields.fromBundle(extras)

        // Use an unmanaged task so that it can be serialized by GSON
        val taskManaged = tasksHelper.getTask(localTaskId, realm, false)
        val localTask = taskManaged?.let { realm.copyFromRealm(it) }

        // Local task was not found, it was probably deleted, no point in continuing
        if (localTask == null) {
            return Result.SUCCESS
        }

        Timber.v("Creating task: $localTask")

        val onlineTask: Task
        try {
            onlineTask =
                if (taskFields != null) {
                    tasksApi.createTask(taskListId, taskFields).blockingGet()
                } else {
                    tasksApi.createTask(taskListId, localTask).blockingGet()
                }
        } catch (ex: Exception) {
            // Handle failure
            Timber.e("Failed to create the new task, will retry...")
            return Result.RESCHEDULE
        }

        // Copy the custom attributes, since they might have changed in the meantime
        onlineTask.copyCustomAttributes(localTask)
        onlineTask.taskListId = taskListId
        onlineTask.synced = true
        // Update the local task with the full information and delete the old task
        realm.executeTransaction {
            it.insertOrUpdate(onlineTask)
            taskManaged.deleteFromRealm()
        }

        // Update the widget
        widgetHelper.updateWidgets(taskListId)

        // Update the previous notification with the correct task ID
        // as long the notification hasn't been dismissed
        if (!onlineTask.notificationDismissed) {
            notificationHelper.scheduleTaskNotification(onlineTask, onlineTask.notificationId)
        }

        // Save the reminder online
        val onlineTaskId = onlineTask.id
        onlineTask.reminder?.let {
            val tasksDatabase = FirebaseDatabase.getInstance().getTasksDatabase()
            tasksDatabase.saveReminder(onlineTaskId, it)
        }

        Timber.d("Create Task job success - %s", onlineTaskId)
        return Result.SUCCESS
    }

    override fun onCancel() {
        Timber.e("Create Task job cancelled")
//        throwable?.let { Timber.e(it.toString()) }
    }
}
