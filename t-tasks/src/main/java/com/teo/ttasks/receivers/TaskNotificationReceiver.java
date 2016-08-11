package com.teo.ttasks.receivers;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import com.teo.ttasks.TTasksApp;
import com.teo.ttasks.data.remote.TasksHelper;

import javax.inject.Inject;

import io.realm.Realm;
import rx.android.schedulers.AndroidSchedulers;
import timber.log.Timber;

public class TaskNotificationReceiver extends BroadcastReceiver {

    public static final String NOTIFICATION_ID = "notification-id";
    public static final String NOTIFICATION = "notification";

    public static final String TASK_ID = "taskId";
    public static final String TASK_LIST_ID = "taskListId";

    public static final String ACTION_PUBLISH = "publish";
    public static final String ACTION_COMPLETE = "complete";

    @Inject TasksHelper tasksHelper;

    @Override
    public void onReceive(Context context, Intent intent) {

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        int id = intent.getIntExtra(NOTIFICATION_ID, 0);

        switch (intent.getAction()) {
            case ACTION_PUBLISH:
                Notification notification = intent.getParcelableExtra(NOTIFICATION);
                notificationManager.notify(id, notification);
                break;
            case ACTION_COMPLETE:
                TTasksApp.get(context).applicationComponent().inject(this);
                String taskId = intent.getStringExtra(TASK_ID);
                String taskListId = intent.getStringExtra(TASK_LIST_ID);
                Realm realm = Realm.getDefaultInstance();
                tasksHelper.updateCompletionStatus(taskListId, taskId, realm)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                tTask -> {
                                    Toast.makeText(context, "Task completed", Toast.LENGTH_SHORT).show();
                                    // Update successful, update sync status
                                    realm.executeTransaction(realm1 -> tTask.setSynced(true));
                                    realm.close();
                                    notificationManager.cancel(id);
                                },
                                throwable -> {
                                    Timber.e(throwable.toString());
                                    Toast.makeText(context, "Error: task not found", Toast.LENGTH_SHORT).show();
                                    realm.close();
                                    notificationManager.cancel(id);
                                }
                        );
                break;
        }
    }
}
