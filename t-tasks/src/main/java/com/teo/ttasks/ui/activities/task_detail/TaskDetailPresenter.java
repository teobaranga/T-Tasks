package com.teo.ttasks.ui.activities.task_detail;

import android.support.annotation.NonNull;

import com.birbit.android.jobqueue.Job;
import com.birbit.android.jobqueue.JobManager;
import com.birbit.android.jobqueue.callback.JobManagerCallback;
import com.birbit.android.jobqueue.callback.JobManagerCallbackAdapter;
import com.teo.ttasks.data.local.PrefHelper;
import com.teo.ttasks.data.local.WidgetHelper;
import com.teo.ttasks.data.model.TTask;
import com.teo.ttasks.data.remote.TasksHelper;
import com.teo.ttasks.jobs.CreateTaskJob;
import com.teo.ttasks.jobs.DeleteTaskJob;
import com.teo.ttasks.ui.base.Presenter;
import com.teo.ttasks.util.NotificationHelper;

import io.reactivex.Flowable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.realm.Realm;
import timber.log.Timber;

public class TaskDetailPresenter extends Presenter<TaskDetailView> {

    private final TasksHelper tasksHelper;
    private final PrefHelper prefHelper;
    private final WidgetHelper widgetHelper;
    private final NotificationHelper notificationHelper;
    private final JobManager jobManager;

    private JobManagerCallback jobManagerCallback;

    private Disposable taskSubscription;

    private Realm realm;

    private TTask tTask;

    String taskId;

    public TaskDetailPresenter(TasksHelper tasksHelper, PrefHelper prefHelper, WidgetHelper widgetHelper,
                               NotificationHelper notificationHelper, JobManager jobManager) {
        this.tasksHelper = tasksHelper;
        this.prefHelper = prefHelper;
        this.widgetHelper = widgetHelper;
        this.notificationHelper = notificationHelper;
        this.jobManager = jobManager;
    }

    void getTask(String taskId) {
        this.taskId = taskId;
        if (taskSubscription != null && !taskSubscription.isDisposed())
            taskSubscription.dispose();
        taskSubscription = tasksHelper.getTaskAsFlowable(taskId, realm)
                .subscribe(
                        // Realm observables do not throw errors
                        tTask -> {
                            final TaskDetailView view = view();
                            if (tTask == null) {
                                if (view != null) view.onTaskLoadError();
                            } else {
                                if (!tTask.isValid()) return;
                                this.tTask = tTask;
                                if (view != null) view.onTaskLoaded(tTask);
                            }
                        }
                );

    }

    void getTaskList(String taskListId) {
        tasksHelper.getTaskListAsFlowable(taskListId, realm)
                .subscribe(
                        // Realm observables do not throw errors
                        taskList -> {
                            final TaskDetailView view = view();
                            if (view != null) {
                                if (taskList == null) view.onTaskListLoadError();
                                else view.onTaskListLoaded(taskList);
                            }
                        }
                );
    }

    /**
     * Mark the task as completed if it isn't and vice versa.
     * If the task is completed, the completion date is set to the current date.
     */
    void updateCompletionStatus() {
        tasksHelper.updateCompletionStatus(tTask, realm)
                .subscribe(
                        task -> { },
                        throwable -> {
                            // Update unsuccessful, keep the task marked as "not synced"
                            // The app will retry later, as soon as the user is online
                            Timber.e(throwable.toString());
                        }
                );

        // Trigger a widget update only if the task is marked as active
        if (!tTask.isCompleted())
            widgetHelper.updateWidgets(tTask.getTaskListId());

        final TaskDetailView view = view();
        if (view != null) view.onTaskUpdated(tTask);
        if (!tTask.isCompleted())
            notificationHelper.scheduleTaskNotification(tTask);
    }

    /**
     * Delete the task
     */
    void deleteTask() {
        final boolean isCompleted = tTask.isCompleted();
        final int notificationId = tTask.getNotificationId();
        final String taskId = tTask.getId();
        final String taskListId = tTask.getTaskListId();

        if (tTask.isLocalOnly()) {
            // Delete the task from the local database
            realm.executeTransaction(realm1 -> TTask.deleteFromRealm(tTask));

            // Make the last local task ID reusable
            prefHelper.deleteLastTaskId();

        } else {
            // Mark it as deleted so it doesn't show up in the list
            realm.executeTransaction(realm -> tTask.setDeleted(true));

            jobManager.addJobInBackground(new DeleteTaskJob(taskId, taskListId));
        }

        // Trigger a widget update only if the task is marked as active
        if (!isCompleted)
            widgetHelper.updateWidgets(taskListId);

        // Cancel the notification, if present
        notificationHelper.cancelTaskNotification(notificationId);

        final TaskDetailView view = view();
        if (view != null) view.onTaskDeleted();
    }

    @Override
    public void bindView(@NonNull TaskDetailView view) {
        super.bindView(view);
        realm = Realm.getDefaultInstance();
        jobManagerCallback = new JobManagerCallbackAdapter() {
            @Override public void onJobRun(@NonNull Job job, int resultCode) {
                Flowable.defer(() -> {
                    if (job instanceof CreateTaskJob) {
                        final CreateTaskJob createTaskJob = (CreateTaskJob) job;
                        if (createTaskJob.getLocalTaskId().equals(taskId) && resultCode == RESULT_SUCCEED) {
                            // Update the task
                            getTask(createTaskJob.getOnlineTaskId());
                        }
                    }
                    return Flowable.empty();
                }).subscribeOn(AndroidSchedulers.mainThread()).subscribe();
            }
        };
        jobManager.addCallback(jobManagerCallback);
    }

    @Override
    public void unbindView(@NonNull TaskDetailView view) {
        super.unbindView(view);
        realm.close();
        jobManager.removeCallback(jobManagerCallback);
        jobManagerCallback = null;
    }
}
