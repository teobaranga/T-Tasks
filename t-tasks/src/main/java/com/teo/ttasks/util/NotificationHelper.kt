package com.teo.ttasks.util

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.preference.PreferenceManager
import androidx.core.app.NotificationCompat
import androidx.core.app.TaskStackBuilder
import androidx.core.content.res.ResourcesCompat
import com.teo.ttasks.R
import com.teo.ttasks.data.model.Task
import com.teo.ttasks.receivers.TaskNotificationReceiver
import com.teo.ttasks.ui.activities.task_detail.TaskDetailActivity
import timber.log.Timber

class NotificationHelper(private val context: Context) {

    /**
     * Schedule a notification to show up at the task's reminder date and time.
     * Does nothing if the reminder date doesn't exist or if the task is already completed.

     * @param task task
     * *
     * @param id   notification ID
     */
    @JvmOverloads
    fun scheduleTaskNotification(task: Task, id: Int = task.notificationId) {
        if (!task.isCompleted && task.reminder != null) {
            val notificationId = if (id != 0) id else task.notificationId

            Timber.d("scheduling notification for %s with id %d", task.id, notificationId)

            // Creates an explicit intent for an Activity in your app
            val resultIntent = TaskDetailActivity.getStartIntent(context, task.id, task.taskListId, true)

            // The stack builder object will contain an artificial back stack for the started Activity.
            // This ensures that navigating backward from the Activity leads out of
            // your application to the Home screen.
            val stackBuilder = TaskStackBuilder.create(context)
            // Adds the back stack for the Intent (but not the Intent itself)
            stackBuilder.addParentStack(TaskDetailActivity::class.java)
            // Adds the Intent that starts the Activity to the top of the stack
            stackBuilder.addNextIntent(resultIntent)
            val resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT)

            // Set the "mark as completed" action
            val completedIntent = Intent(context, TaskNotificationReceiver::class.java)
            completedIntent.putExtra(TaskNotificationReceiver.NOTIFICATION_ID, notificationId)
            completedIntent.putExtra(TaskNotificationReceiver.TASK_ID, task.id)
            completedIntent.action = TaskNotificationReceiver.ACTION_COMPLETE
            val completedPendingIntent = PendingIntent.getBroadcast(context, 0, completedIntent, PendingIntent.FLAG_UPDATE_CURRENT)
            val completedAction = NotificationCompat.Action(R.drawable.ic_done_white_24dp, "Mark as completed", completedPendingIntent)

            // Create the delete intent
            val deleteIntent = Intent(context, TaskNotificationReceiver::class.java)
            deleteIntent.putExtra(TaskNotificationReceiver.TASK_ID, task.id)
            deleteIntent.action = TaskNotificationReceiver.ACTION_DELETE
            val deletePendingIntent = PendingIntent.getBroadcast(context, 0, deleteIntent, PendingIntent.FLAG_UPDATE_CURRENT)

            // Check if user enabled reminder vibration
            val preferences = PreferenceManager.getDefaultSharedPreferences(context)
            val vibrate = preferences.getBoolean("reminder_vibrate", true)
            val sound = Uri.parse(preferences.getString("reminder_sound", context.getString(R.string.default_reminder_sound)))
            val colorString = preferences.getString("reminder_color", context.getString(R.string.default_led_color))
            val color = if (colorString!!.isEmpty()) NotificationCompat.COLOR_DEFAULT else Color.parseColor(colorString)

            // Create the notification
            val notification = NotificationCompat.Builder(context, "default")
                    .setContentTitle(String.format(context.getString(R.string.task_due), task.title))
                    .setContentText(context.getString(R.string.notification_more_info))
                    .setSmallIcon(R.drawable.ic_assignment_turned_in_24dp)
                    .setDefaults(if (vibrate) Notification.DEFAULT_VIBRATE else 0) // enable vibration only if requested
                    .setSound(sound)
                    .setOnlyAlertOnce(true)
                    .setLights(color, 500, 2000)
                    .setColor(ResourcesCompat.getColor(context.resources, R.color.colorPrimary, null))
                    .setContentIntent(resultPendingIntent)
                    .setDeleteIntent(deletePendingIntent)
                    .addAction(completedAction)
                    .setAutoCancel(true)
                    .build()

            val notificationIntent = Intent(context, TaskNotificationReceiver::class.java)
            notificationIntent.putExtra(TaskNotificationReceiver.NOTIFICATION_ID, notificationId)
            notificationIntent.putExtra(TaskNotificationReceiver.NOTIFICATION, notification)
            notificationIntent.action = TaskNotificationReceiver.ACTION_PUBLISH
            val pendingIntent = PendingIntent.getBroadcast(context, notificationId, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT)

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.set(AlarmManager.RTC_WAKEUP, task.reminder!!.time, pendingIntent)
        }
    }

    /**
     * Cancel a notification. Used when deleting a task.

     * @param id notification ID
     */
    fun cancelTaskNotification(id: Int) {
        // Cancel scheduled notification
        val notificationIntent = Intent(context, TaskNotificationReceiver::class.java)
        notificationIntent.action = TaskNotificationReceiver.ACTION_PUBLISH
        val pendingIntent = PendingIntent.getBroadcast(context, id, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent)

        // Cancel active notification
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(id)
    }
}
