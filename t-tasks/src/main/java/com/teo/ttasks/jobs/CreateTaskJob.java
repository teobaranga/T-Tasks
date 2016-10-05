package com.teo.ttasks.jobs;

import android.annotation.SuppressLint;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.birbit.android.jobqueue.CancelReason;
import com.birbit.android.jobqueue.Job;
import com.birbit.android.jobqueue.Params;
import com.birbit.android.jobqueue.RetryConstraint;
import com.google.firebase.database.DatabaseReference;
import com.teo.ttasks.TTasksApp;
import com.teo.ttasks.data.local.PrefHelper;
import com.teo.ttasks.data.local.WidgetHelper;
import com.teo.ttasks.data.model.TTask;
import com.teo.ttasks.data.model.TaskFields;
import com.teo.ttasks.data.remote.TasksHelper;
import com.teo.ttasks.util.FirebaseUtil;
import com.teo.ttasks.util.NotificationHelper;

import javax.inject.Inject;

import io.realm.Realm;
import timber.log.Timber;

public class CreateTaskJob extends Job {

    @Inject TasksHelper tasksHelper;
    @Inject PrefHelper prefHelper;
    @Inject WidgetHelper widgetHelper;
    @Inject NotificationHelper notificationHelper;

    private String localTaskId;
    private String onlineTaskId;
    private String taskListId;
    private TaskFields taskFields;

    public CreateTaskJob(String localTaskId, String taskListId, TaskFields taskFields) {
        super(new Params(Priority.MID).requireNetwork().persist());
        this.localTaskId = localTaskId;
        this.taskListId = taskListId;
        this.taskFields = taskFields;
    }

    @Override
    public void onAdded() {
        // Do nothing
    }

    @Override @SuppressLint("NewApi")
    public void onRun() throws Throwable {
        TTasksApp.get(getApplicationContext()).userComponent().inject(this);
        tasksHelper.newTask(taskListId, taskFields)
                .toBlocking() // Bring this back on the same thread as the job
                .subscribe(
                        savedTask -> {
                            final Realm realm = Realm.getDefaultInstance();
                            final TTask localTask = realm.where(TTask.class).equalTo("id", localTaskId).findFirst();
                            final TTask onlineTask = new TTask(localTask, savedTask);
                            onlineTask.setSynced(true);
                            // Update the local task with the full information and delete the old task
                            final int id = localTask.hashCode();
                            realm.executeTransaction(realm1 -> {
                                realm.insertOrUpdate(onlineTask);
                                localTask.getTask().deleteFromRealm();
                                localTask.deleteFromRealm();
                            });
                            realm.close();

                            // Recover the task ID so that it can be reused
                            prefHelper.deleteLastTaskId();

                            // Update the widget
                            widgetHelper.updateWidgets(taskListId);

                            // Update the previous notification with the correct task ID
                            // as long the notification hasn't been dismissed
                            if (!onlineTask.isNotificationDismissed()) {
                                notificationHelper.scheduleTaskNotification(onlineTask, id);
                            }

                            // Save the reminder online
                            onlineTaskId = onlineTask.getId();
                            if (onlineTask.getReminder() != null) {
                                final DatabaseReference tasksDatabase = FirebaseUtil.getTasksDatabase();
                                tasksDatabase.child(onlineTaskId).child("reminder").setValue(onlineTask.getReminder().getTime());
                            }

                            Timber.d("new task with id %s", onlineTaskId);
                        },
                        throwable -> Timber.e(throwable.toString())
                );
    }

    @Override
    protected void onCancel(@CancelReason int cancelReason, @Nullable Throwable throwable) {
        if (throwable != null)
            Timber.e(throwable.toString());
    }

    @Override
    protected RetryConstraint shouldReRunOnThrowable(@NonNull Throwable throwable, int runCount, int maxRunCount) {
        return RetryConstraint.createExponentialBackoff(runCount, 1000);
    }

    public String getLocalTaskId() {
        return localTaskId;
    }

    public String getOnlineTaskId() {
        return onlineTaskId;
    }
}
