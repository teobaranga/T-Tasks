package com.teo.ttasks.receivers

import android.app.Notification
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.teo.ttasks.data.remote.TasksHelper
import io.realm.Realm
import org.koin.core.KoinComponent
import org.koin.core.inject
import timber.log.Timber

class TaskNotificationReceiver : BroadcastReceiver(), KoinComponent {

    companion object {
        const val NOTIFICATION_ID = "notification-id"
        const val NOTIFICATION = "notification"

        const val TASK_ID = "taskId"

        const val ACTION_PUBLISH = "publish"
        const val ACTION_COMPLETE = "complete"
        const val ACTION_DELETE = "delete"
    }

    private val tasksHelper: TasksHelper by inject()

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
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
                if (tasksHelper.updateCompletionStatus(taskId!!, realm)) {
                    Toast.makeText(context, "Task completed", Toast.LENGTH_SHORT).show()
                    notificationManager.cancel(id)
                } else {
                    Toast.makeText(context, "Task not found. This is the case if the task was deleted.", Toast.LENGTH_SHORT).show()
                }
                notificationManager.cancel(id)
                realm.close()
            }
            ACTION_DELETE -> {
                // Mark this task's reminder as dismissed
                val realm = Realm.getDefaultInstance()
                tasksHelper.getTaskAsSingle(taskId!!, realm)
                        .subscribe(
                                { task ->
                                    realm.executeTransaction { task.notificationDismissed = true }
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
