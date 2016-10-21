package com.teo.ttasks.util;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.res.ResourcesCompat;

import com.teo.ttasks.R;
import com.teo.ttasks.data.model.TTask;
import com.teo.ttasks.receivers.TaskNotificationReceiver;
import com.teo.ttasks.ui.activities.task_detail.TaskDetailActivity;

import timber.log.Timber;

public class NotificationHelper {

    private final Context context;

    public NotificationHelper(Context applicationContext) {
        this.context = applicationContext;
    }

    /**
     * Schedule a notification to show up at the task's reminder date and time.
     * Does nothing if the reminder date doesn't exist or if the task is already completed.
     *
     * @param task task
     * @param id   notification ID
     */
    public void scheduleTaskNotification(TTask task, int id) {
        if (!task.isCompleted() && task.getReminder() != null) {
            int notificationId = id != 0 ? id : task.getNotificationId();

            Timber.d("scheduling notification for %s with id %d", task.getId(), notificationId);

            // Creates an explicit intent for an Activity in your app
            Intent resultIntent = TaskDetailActivity.getStartIntent(context, task.getId(), task.getTaskListId(), true);

            // The stack builder object will contain an artificial back stack for the started Activity.
            // This ensures that navigating backward from the Activity leads out of
            // your application to the Home screen.
            TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
            // Adds the back stack for the Intent (but not the Intent itself)
            stackBuilder.addParentStack(TaskDetailActivity.class);
            // Adds the Intent that starts the Activity to the top of the stack
            stackBuilder.addNextIntent(resultIntent);
            PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

            // Set the "mark as completed" action
            Intent completedIntent = new Intent(context, TaskNotificationReceiver.class);
            completedIntent.putExtra(TaskNotificationReceiver.NOTIFICATION_ID, notificationId);
            completedIntent.putExtra(TaskNotificationReceiver.TASK_ID, task.getId());
            completedIntent.setAction(TaskNotificationReceiver.ACTION_COMPLETE);
            PendingIntent completedPendingIntent = PendingIntent.getBroadcast(context, 0, completedIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            NotificationCompat.Action completedAction = new NotificationCompat.Action(R.drawable.ic_done_white_24dp, "Mark as completed", completedPendingIntent);

            // Create the delete intent
            final Intent deleteIntent = new Intent(context, TaskNotificationReceiver.class);
            deleteIntent.putExtra(TaskNotificationReceiver.TASK_ID, task.getId());
            deleteIntent.setAction(TaskNotificationReceiver.ACTION_DELETE);
            final PendingIntent deletePendingIntent = PendingIntent.getBroadcast(context, 0, deleteIntent, PendingIntent.FLAG_UPDATE_CURRENT);

            // Check if user enabled reminder vibration
            final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
            final boolean vibrate = preferences.getBoolean("reminder_vibrate", true);
            final Uri sound = Uri.parse(preferences.getString("reminder_sound", context.getString(R.string.default_reminder_sound)));
            final String colorString = preferences.getString("reminder_color", context.getString(R.string.default_led_color));
            final int color = colorString.isEmpty() ? NotificationCompat.COLOR_DEFAULT : Color.parseColor(colorString);

            // Create the notification
            Notification notification = new NotificationCompat.Builder(context)
                    .setContentTitle(String.format(context.getString(R.string.task_due), task.getTitle()))
                    .setContentText(context.getString(R.string.notification_more_info))
                    .setSmallIcon(R.drawable.ic_assignment_turned_in_24dp)
                    .setDefaults((vibrate ? Notification.DEFAULT_VIBRATE : 0)) // enable vibration only if requested
                    .setSound(sound)
                    .setOnlyAlertOnce(true)
                    .setLights(color, 500, 2000)
                    .setColor(ResourcesCompat.getColor(context.getResources(), R.color.colorPrimary, null))
                    .setContentIntent(resultPendingIntent)
                    .setDeleteIntent(deletePendingIntent)
                    .addAction(completedAction)
                    .setAutoCancel(true)
                    .build();

            Intent notificationIntent = new Intent(context, TaskNotificationReceiver.class);
            notificationIntent.putExtra(TaskNotificationReceiver.NOTIFICATION_ID, notificationId);
            notificationIntent.putExtra(TaskNotificationReceiver.NOTIFICATION, notification);
            notificationIntent.setAction(TaskNotificationReceiver.ACTION_PUBLISH);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, notificationId, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            alarmManager.set(AlarmManager.RTC_WAKEUP, task.getReminder().getTime(), pendingIntent);
        }
    }

    public void scheduleTaskNotification(TTask task) {
        scheduleTaskNotification(task, task.getNotificationId());
    }

    /**
     * Cancel a notification. Used when deleting a task.
     *
     * @param id notification ID
     */
    public void cancelTaskNotification(int id) {
        // Cancel scheduled notification
        Intent notificationIntent = new Intent(context, TaskNotificationReceiver.class);
        notificationIntent.setAction(TaskNotificationReceiver.ACTION_PUBLISH);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, id, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(pendingIntent);

        // Cancel active notification
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(id);
    }
}
