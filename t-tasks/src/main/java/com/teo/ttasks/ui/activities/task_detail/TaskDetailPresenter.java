package com.teo.ttasks.ui.activities.task_detail;

import android.support.annotation.NonNull;

import com.google.firebase.database.DatabaseReference;
import com.teo.ttasks.data.local.WidgetHelper;
import com.teo.ttasks.data.model.TTask;
import com.teo.ttasks.data.remote.TasksHelper;
import com.teo.ttasks.ui.base.Presenter;
import com.teo.ttasks.util.FirebaseUtil;
import com.teo.ttasks.util.NotificationHelper;

import io.realm.Realm;
import timber.log.Timber;

public class TaskDetailPresenter extends Presenter<TaskDetailView> {

    private final TasksHelper tasksHelper;
    private final WidgetHelper widgetHelper;
    private final NotificationHelper notificationHelper;

    private Realm realm;

    private TTask tTask;

    public TaskDetailPresenter(TasksHelper tasksHelper, WidgetHelper widgetHelper, NotificationHelper notificationHelper) {
        this.tasksHelper = tasksHelper;
        this.widgetHelper = widgetHelper;
        this.notificationHelper = notificationHelper;
    }

    void getTask(String taskId) {
        tasksHelper.getTask(taskId, realm)
                .subscribe(
                        // Realm observables do not throw errors
                        tTask -> {
                            final TaskDetailView view = view();
                            if (tTask == null) {
                                if (view != null) view.onTaskLoadError();
                            } else {
                                this.tTask = tTask;
                                if (view != null) view.onTaskLoaded(tTask);
                            }
                        }
                );

    }

    void getTaskList(String taskListId) {
        tasksHelper.getTaskList(taskListId, realm)
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
        // Mark it as deleted so it doesn't show up in the list
        realm.executeTransaction(realm -> tTask.setDeleted(true));

        // Delete the reminder
        final DatabaseReference tasksDatabase = FirebaseUtil.getTasksDatabase();
        FirebaseUtil.saveReminder(tasksDatabase, tTask.getId(), null);

        // Trigger a widget update only if the task is marked as active
        if (!tTask.isCompleted())
            widgetHelper.updateWidgets(tTask.getTaskListId());

        // Cancel the notification, if present
        notificationHelper.cancelTaskNotification(tTask.hashCode());

        final TaskDetailView view = view();
        if (view != null) view.onTaskDeleted();

        tasksHelper.deleteTask(tTask.getTaskListId(), tTask.getId())
                .subscribe(
                        aVoid -> { /* Do nothing */ },
                        throwable -> {
                            Timber.e(throwable.toString());
                        }
                );
    }

    @Override
    public void bindView(@NonNull TaskDetailView view) {
        super.bindView(view);
        realm = Realm.getDefaultInstance();
    }

    @Override
    public void unbindView(@NonNull TaskDetailView view) {
        super.unbindView(view);
        realm.close();
    }
}
