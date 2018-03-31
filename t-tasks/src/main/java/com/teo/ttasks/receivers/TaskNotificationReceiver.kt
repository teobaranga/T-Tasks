package com.teo.ttasks.receivers

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.teo.ttasks.data.remote.TasksHelper
import dagger.android.DaggerBroadcastReceiver
import io.reactivex.android.schedulers.AndroidSchedulers
import io.realm.Realm
import timber.log.Timber
import javax.inject.Inject

class TaskNotificationReceiver : DaggerBroadcastReceiver() {

    companion object {
        const val NOTIFICATION_ID = "notification-id"
        const val NOTIFICATION = "notification"

        const val TASK_ID = "taskId"

        const val ACTION_PUBLISH = "publish"
        const val ACTION_COMPLETE = "complete"
        const val ACTION_DELETE = "delete"
    }

    @Inject internal lateinit var tasksHelper: TasksHelper

    override fun onReceive(context: Context, intent: Intent?) {
        super.onReceive(context, intent)

        intent?.action?.let { action ->
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val taskId = intent.getStringExtra(TASK_ID)
            val id = intent.getIntExtra(NOTIFICATION_ID, 0)
            when (action) {
                ACTION_PUBLISH -> {
                    // Display the notification
                    val notification = intent.getParcelableExtra<Notification>(NOTIFICATION)
                    notificationManager.notify(id, notification)
                }
                ACTION_COMPLETE -> {
                    val realm = Realm.getDefaultInstance()
                    // Mark the task as completed
                    tasksHelper.updateCompletionStatus(taskId, realm)
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(
                                    { tTask ->
                                        // Update successful, update sync status
                                        realm.executeTransaction { tTask.synced = true }
                                        Toast.makeText(context, "Task completed", Toast.LENGTH_SHORT).show()
                                        realm.close()
                                        notificationManager.cancel(id)
                                    },
                                    { throwable ->
                                        Timber.e(throwable, "Error while completing %s", taskId)
                                        Toast.makeText(context, "Task not found. This is the case if the task was deleted.", Toast.LENGTH_SHORT).show()
                                        realm.close()
                                        notificationManager.cancel(id)
                                    }
                            )
                }
                ACTION_DELETE -> {
                    // Mark this task's reminder as dismissed
                    val realm = Realm.getDefaultInstance()
                    tasksHelper.getTaskAsSingle(taskId, realm)
                            .subscribe(
                                    { tTask ->
                                        realm.executeTransaction { tTask.notificationDismissed = true }
                                        realm.close()
                                        Timber.v("Dismissed task %s", taskId)
                                    },
                                    { throwable ->
                                        Timber.e(throwable, "Error while dismissing notification for %s", taskId)
                                        realm.close()
                                    }
                            )
                }
                else -> {
                    // do nothing
                }
            }
        }
    }
}
