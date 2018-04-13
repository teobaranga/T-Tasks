package com.teo.ttasks.jobs

import com.evernote.android.job.Job
import com.evernote.android.job.JobManager
import com.evernote.android.job.JobRequest
import com.evernote.android.job.util.support.PersistableBundleCompat
import com.google.firebase.database.FirebaseDatabase
import com.teo.ttasks.TTasksApp
import com.teo.ttasks.api.TasksApi
import com.teo.ttasks.data.local.PrefHelper
import com.teo.ttasks.data.local.TaskFields
import com.teo.ttasks.data.local.WidgetHelper
import com.teo.ttasks.data.model.TTask
import com.teo.ttasks.data.model.Task
import com.teo.ttasks.data.remote.TasksHelper
import com.teo.ttasks.delete
import com.teo.ttasks.util.FirebaseUtil.getTasksDatabase
import com.teo.ttasks.util.FirebaseUtil.saveReminder
import com.teo.ttasks.util.NotificationHelper
import io.realm.Realm
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class CreateTaskJob : Job() {

    companion object {
        const val TAG: String = "CREATE"
        const val EXTRA_LOCAL_ID: String = "localId"
        const val EXTRA_TASK_LIST_ID: String = "taskListId"

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

    @Inject @Transient internal lateinit var tasksHelper: TasksHelper
    @Inject @Transient internal lateinit var prefHelper: PrefHelper
    @Inject @Transient internal lateinit var widgetHelper: WidgetHelper
    @Inject @Transient internal lateinit var notificationHelper: NotificationHelper
    @Inject @Transient internal lateinit var tasksApi: TasksApi

    override fun onRunJob(params: Params): Result {
        // Inject dependencies
        (context.applicationContext as TTasksApp).applicationComponent().inject(this)

        // Extract params
        val extras = params.extras
        val localTaskId = extras[EXTRA_LOCAL_ID] as String
        val taskListId = extras[EXTRA_TASK_LIST_ID] as String
        val taskFields = TaskFields.fromBundle(extras)

        val realm = Realm.getDefaultInstance()
        val localTask = tasksHelper.getTask(localTaskId, realm)

        // Local task was not found, it was probably deleted, no point in continuing
        if (localTask == null) {
            realm.close()
            return Job.Result.SUCCESS
        }

        val savedTask: Task
        try {
            savedTask =
                    if (taskFields != null) {
                        tasksApi.createTask(taskListId, taskFields).blockingGet()
                    } else {
                        tasksApi.createTask(taskListId, localTask.task).blockingGet()
                    }
        } catch (ex: Exception) {
            // Handle failure
            Timber.e("Failed to create the new task, will retry...")
            return Job.Result.RESCHEDULE
        }

        // Create the task that will be saved online
        val onlineTask = TTask(localTask, savedTask)
        onlineTask.synced = true
        // Update the local task with the full information and delete the old task
        realm.executeTransaction {
            it.insertOrUpdate(onlineTask)
            localTask.delete()
        }
        realm.close()

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
            tasksDatabase.saveReminder(onlineTaskId, it.time)
        }

        Timber.d("Create Task job success - %s", onlineTaskId)
        return Job.Result.SUCCESS
    }

    override fun onCancel() {
        Timber.e("Create Task job cancelled")
//        throwable?.let { Timber.e(it.toString()) }
    }
}
