package com.teo.ttasks.util;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import com.teo.ttasks.R;
import com.teo.ttasks.data.model.TTask;
import com.teo.ttasks.data.model.Task;
import com.teo.ttasks.receivers.NotificationPublisher;

import java.util.Date;

public class NotificationUtils {

    private NotificationUtils() { }

    public static void scheduleNotification(Context context, Notification notification, Date date) {

        Intent notificationIntent = new Intent(context, NotificationPublisher.class);
        notificationIntent.putExtra(NotificationPublisher.NOTIFICATION_ID, 1);
        notificationIntent.putExtra(NotificationPublisher.NOTIFICATION, notification);
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
     * @param context context
     * @param task task
     */
    public static void scheduleTaskNotification(Context context, TTask task) {
        if (task.getReminder() != null) {
            // Create the notification
            Notification notification = new Notification.Builder(context)
                    .setContentTitle(task.getTitle())
                    .setContentText("Click for more information")
                    .setSmallIcon(R.drawable.ic_assignment_turned_in_24dp)
                    .build();

            Intent notificationIntent = new Intent(context, NotificationPublisher.class);
            notificationIntent.putExtra(NotificationPublisher.NOTIFICATION_ID, 1);
            notificationIntent.putExtra(NotificationPublisher.NOTIFICATION, notification);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            alarmManager.set(AlarmManager.RTC_WAKEUP, task.getReminder().getTime(), pendingIntent);
        }
    }
}
