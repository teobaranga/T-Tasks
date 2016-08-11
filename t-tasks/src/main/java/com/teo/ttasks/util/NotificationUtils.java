package com.teo.ttasks.util;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;

import com.teo.ttasks.R;
import com.teo.ttasks.data.model.TTask;
import com.teo.ttasks.data.model.Task;
import com.teo.ttasks.receivers.TaskNotificationReceiver;
import com.teo.ttasks.ui.activities.task_detail.TaskDetailActivity;

import java.util.Date;

public class NotificationUtils {

    private NotificationUtils() { }

    public static void scheduleNotification(Context context, Notification notification, Date date) {

        Intent notificationIntent = new Intent(context, TaskNotificationReceiver.class);
        notificationIntent.putExtra(TaskNotificationReceiver.NOTIFICATION_ID, 1);
        notificationIntent.putExtra(TaskNotificationReceiver.NOTIFICATION, notification);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.set(AlarmManager.RTC_WAKEUP, date.getTime(), pendingIntent);
    }

    public static Notification getTaskNotification(Context context, Task task) {
        Notification.Builder builder = new Notification.Builder(context)
                .setContentTitle(task.getTitle())
                .setContentText("Click for more information")
                .setSmallIcon(R.drawable.ic_assignment_turned_in_24dp);
        return builder.build();
    }

    /**
     * Schedule a notification to show up at the task's reminder date and time.
     * Does nothing if the reminder date doesn't exist.
     *
     * @param context context
     * @param task    task
     */
    public static void scheduleTaskNotification(Context context, TTask task) {
        if (task.getReminder() != null) {
            int notificationId = 1;

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
            completedIntent.putExtra(TaskNotificationReceiver.TASK_LIST_ID, task.getTaskListId());
            completedIntent.setAction(TaskNotificationReceiver.ACTION_COMPLETE);
            PendingIntent completedPendingIntent = PendingIntent.getBroadcast(context, 0, completedIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            NotificationCompat.Action completedAction = new NotificationCompat.Action(R.drawable.ic_done_white_24dp, "Mark as completed", completedPendingIntent);

            // Create the notification
            Notification notification = new NotificationCompat.Builder(context)
                    .setContentTitle(String.format(context.getString(R.string.task_due), task.getTitle()))
                    .setContentText(context.getString(R.string.notification_more_info))
                    .setSmallIcon(R.drawable.ic_assignment_turned_in_24dp)
                    .setContentIntent(resultPendingIntent)
                    .addAction(completedAction)
                    .setAutoCancel(true)
                    .build();

            Intent notificationIntent = new Intent(context, TaskNotificationReceiver.class);
            notificationIntent.putExtra(TaskNotificationReceiver.NOTIFICATION_ID, notificationId);
            notificationIntent.putExtra(TaskNotificationReceiver.NOTIFICATION, notification);
            notificationIntent.setAction(TaskNotificationReceiver.ACTION_PUBLISH);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            alarmManager.set(AlarmManager.RTC_WAKEUP, task.getReminder().getTime(), pendingIntent);
        }
    }
}
