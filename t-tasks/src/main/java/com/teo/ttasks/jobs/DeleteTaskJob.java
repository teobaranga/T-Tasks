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
import com.teo.ttasks.data.model.TTask;
import com.teo.ttasks.data.remote.TasksHelper;
import com.teo.ttasks.util.FirebaseUtil;

import javax.inject.Inject;

import io.realm.Realm;
import timber.log.Timber;

public class DeleteTaskJob extends Job {

    @Inject TasksHelper tasksHelper;

    private String taskId;
    private String taskListId;

    public DeleteTaskJob(String taskId, String taskListId) {
        super(new Params(Priority.MID).requireNetwork().persist());
        this.taskId = taskId;
        this.taskListId = taskListId;
    }

    @Override
    public void onAdded() {
        // Do nothing
    }

    @Override @SuppressLint("NewApi")
    public void onRun() throws Throwable {
        TTasksApp.get(getApplicationContext()).userComponent().inject(this);

        // Delete the reminder
        final DatabaseReference tasksDatabase = FirebaseUtil.getTasksDatabase();
        FirebaseUtil.saveReminder(tasksDatabase, taskId, null);

        // Delete the Google task
        tasksHelper.deleteTask(taskListId, taskId)
                .toBlocking() // Bring this back on the same thread as the job
                .subscribe(
                        aVoid -> {
                            final Realm realm = Realm.getDefaultInstance();
                            final TTask tTask = realm.where(TTask.class).equalTo("id", taskId).findFirst();
                            // Delete the Realm task
                            if (tTask != null) {
                                realm.executeTransaction(realm1 -> {
                                    tTask.getTask().deleteFromRealm();
                                    tTask.deleteFromRealm();
                                });
                            }
                            realm.close();

                            Timber.d("Task %s deleted", taskId);
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
}
