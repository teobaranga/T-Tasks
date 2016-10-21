package com.teo.ttasks.jobs;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.birbit.android.jobqueue.CancelReason;
import com.birbit.android.jobqueue.Job;
import com.birbit.android.jobqueue.Params;
import com.birbit.android.jobqueue.RetryConstraint;
import com.google.firebase.database.DatabaseReference;
import com.teo.ttasks.api.TasksApi;
import com.teo.ttasks.data.local.PrefHelper;
import com.teo.ttasks.data.local.WidgetHelper;
import com.teo.ttasks.data.model.TTask;
import com.teo.ttasks.data.model.Task;
import com.teo.ttasks.data.model.TaskFields;
import com.teo.ttasks.util.FirebaseUtil;
import com.teo.ttasks.util.NotificationHelper;

import javax.inject.Inject;

import io.realm.Realm;
import retrofit2.Response;
import timber.log.Timber;

public class CreateTaskJob extends Job {

    @Inject transient PrefHelper prefHelper;
    @Inject transient WidgetHelper widgetHelper;
    @Inject transient NotificationHelper notificationHelper;
    @Inject transient TasksApi tasksApi;

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

    @Override
    public void onRun() throws Throwable {
        final Realm realm = Realm.getDefaultInstance();
        final TTask localTask = realm.where(TTask.class).equalTo("id", localTaskId).findFirst();

        // Local task was not found, it was probably deleted, no point in continuing
        if (localTask == null) {
            realm.close();
            return;
        }

        final Response<Task> response = tasksApi.insertTask(taskListId, taskFields).execute();

        // Handle failure
        if (!response.isSuccessful()) {
            response.errorBody().close();
            throw new Exception("Failed to save task");
        }

        final Task savedTask = response.body();

        // Create the task that will be saved online
        final TTask onlineTask = new TTask(localTask, savedTask);
        onlineTask.setSynced(true);
        // Update the local task with the full information and delete the old task
        realm.executeTransaction(realm1 -> {
            realm.insertOrUpdate(onlineTask);
            TTask.deleteFromRealm(localTask);
        });
        realm.close();

        // Recover the task ID so that it can be reused
        prefHelper.deleteLastTaskId();

        // Update the widget
        widgetHelper.updateWidgets(taskListId);

        // Update the previous notification with the correct task ID
        // as long the notification hasn't been dismissed
        if (!onlineTask.isNotificationDismissed()) {
            notificationHelper.scheduleTaskNotification(onlineTask, onlineTask.getNotificationId());
        }

        // Save the reminder online
        onlineTaskId = onlineTask.getId();
        if (onlineTask.getReminder() != null) {
            final DatabaseReference tasksDatabase = FirebaseUtil.getTasksDatabase();
            tasksDatabase.child(onlineTaskId).child("reminder").setValue(onlineTask.getReminder().getTime());
        }

        Timber.d("saved new task with id %s", onlineTaskId);
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
