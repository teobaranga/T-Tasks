package com.teo.ttasks.ui.activities.task_detail;

import android.support.annotation.NonNull;

import com.teo.ttasks.data.model.TTask;
import com.teo.ttasks.data.remote.TasksHelper;
import com.teo.ttasks.ui.base.Presenter;

import io.realm.Realm;
import timber.log.Timber;

public class TaskDetailPresenter extends Presenter<TaskDetailView> {

    private final TasksHelper tasksHelper;

    private Realm realm;

    private TTask tTask;

    public TaskDetailPresenter(TasksHelper tasksHelper) {
        this.tasksHelper = tasksHelper;
    }

    void getTask(String taskId) {
        tasksHelper.getTask(taskId, realm)
                .subscribe(
                        tTask -> {
                            this.tTask = tTask;
                            final TaskDetailView view = view();
                            if (view != null) view.onTaskLoaded(tTask);
                        },
                        throwable -> {
                            Timber.e(throwable.toString());
                            final TaskDetailView view = view();
                            if (view != null) view.onTaskLoadError();
                        }
                );

    }

    void getTaskList(String taskListId) {
        tasksHelper.getTaskList(taskListId, realm)
                .subscribe(
                        taskList -> {
                            final TaskDetailView view = view();
                            if (view != null) view.onTaskListLoaded(taskList);
                        },
                        throwable -> {
                            Timber.e(throwable.toString());
                            final TaskDetailView view = view();
                            if (view != null) view.onTaskListLoadError();
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
                            // TODO: 2016-08-04 provide the user with the option of retrying
                            Timber.e(throwable.toString());
                        }
                );
        final TaskDetailView view = view();
        if (view != null) view.onTaskUpdated();
    }

    /**
     * Delete the task
     */
    void deleteTask() {
        tasksHelper.deleteTask(tTask.getTaskListId(), tTask.getId(), realm)
                .subscribe(
                        aVoid -> {
                            final TaskDetailView view = view();
                            if (view != null) view.onTaskDeleted();
                        },
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
