package com.teo.ttasks.jobs;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.birbit.android.jobqueue.CancelReason;
import com.birbit.android.jobqueue.Job;
import com.birbit.android.jobqueue.Params;
import com.birbit.android.jobqueue.RetryConstraint;
import com.google.firebase.database.DatabaseReference;
import com.teo.ttasks.api.TasksApi;
import com.teo.ttasks.data.model.TTask;
import com.teo.ttasks.util.FirebaseUtil;

import javax.inject.Inject;

import io.realm.Realm;
import retrofit2.Response;
import timber.log.Timber;

public class DeleteTaskJob extends Job {

    @Inject transient TasksApi tasksApi;

    private String taskId;
    private String taskListId;

    public DeleteTaskJob(String taskId, String taskListId) {
        super(new Params(Priority.HIGH).requireNetwork().persist());
        this.taskId = taskId;
        this.taskListId = taskListId;
    }

    @Override
    public void onAdded() {
        // Do nothing
    }

    public void onRun() throws Throwable {
        // Delete the reminder
        final DatabaseReference tasksDatabase = FirebaseUtil.getTasksDatabase();
        FirebaseUtil.saveReminder(tasksDatabase, taskId, null);

        final Realm realm = Realm.getDefaultInstance();
        final TTask tTask = realm.where(TTask.class).equalTo("id", taskId).findFirst();

        // Task not found, nothing to do here
        if (tTask == null) {
            realm.close();
            return;
        }

        // Delete the Google task
        if (!tTask.isLocalOnly()) {
            final Response<Void> response = tasksApi.deleteTask(taskListId, taskId).execute();
            response.body();

            // Handle failure
            if (!response.isSuccessful()) {
                realm.close();
                response.errorBody().close();
                throw new Exception("Failed to delete task");
            }
        }

        // Delete the Realm task
        realm.executeTransaction(realm1 -> TTask.deleteFromRealm(tTask));
        Timber.d("deleted task %s", taskId);

        realm.close();
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
}
