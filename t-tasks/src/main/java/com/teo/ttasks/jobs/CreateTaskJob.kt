package com.teo.ttasks.jobs

import com.birbit.android.jobqueue.CancelReason
import com.birbit.android.jobqueue.Job
import com.birbit.android.jobqueue.Params
import com.birbit.android.jobqueue.RetryConstraint
import com.teo.ttasks.api.TasksApi
import com.teo.ttasks.data.local.PrefHelper
import com.teo.ttasks.data.local.TaskFields
import com.teo.ttasks.data.local.WidgetHelper
import com.teo.ttasks.data.model.TTask
import com.teo.ttasks.data.remote.TasksHelper
import com.teo.ttasks.delete
import com.teo.ttasks.util.FirebaseUtil
import com.teo.ttasks.util.NotificationHelper
import io.realm.Realm
import timber.log.Timber
import java.lang.Exception
import javax.inject.Inject

class CreateTaskJob(val localTaskId: String, private val taskListId: String, private val taskFields: TaskFields) :
        Job(Params(Priority.MID).requireNetwork().persist()) {

    @Inject @Transient internal lateinit var tasksHelper: TasksHelper
    @Inject @Transient internal lateinit var prefHelper: PrefHelper
    @Inject @Transient internal lateinit var widgetHelper: WidgetHelper
    @Inject @Transient internal lateinit var notificationHelper: NotificationHelper
    @Inject @Transient internal lateinit var tasksApi: TasksApi

    lateinit var onlineTaskId: String
        private set

    override fun onAdded() {
        // Do nothing
    }

    @Throws(Throwable::class)
    override fun onRun() {
        val realm = Realm.getDefaultInstance()
        val localTask = tasksHelper.getTask(localTaskId, realm)

        // Local task was not found, it was probably deleted, no point in continuing
        if (localTask == null) {
            realm.close()
            return
        }

        val response = tasksApi.insertTask(taskListId, taskFields).execute()

        // Handle failure
        if (!response.isSuccessful) {
            response.errorBody()?.close()
            throw Exception("Failed to save task")
        }

        val savedTask = response.body()!!

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
        onlineTaskId = onlineTask.id
        onlineTask.reminder?.let { FirebaseUtil.saveReminder(onlineTaskId, it.time) }

        Timber.d("saved new task with id %s", onlineTaskId)
    }

    override fun onCancel(@CancelReason cancelReason: Int, throwable: Throwable?) {
        throwable?.let { Timber.e(it.toString()) }
    }

    override fun shouldReRunOnThrowable(throwable: Throwable, runCount: Int, maxRunCount: Int): RetryConstraint {
        return RetryConstraint.createExponentialBackoff(runCount, 1000)
    }
}
