package com.teo.ttasks.ui.activities.task_detail;

import android.support.annotation.NonNull;

import com.teo.ttasks.data.model.Task;
import com.teo.ttasks.data.remote.TasksHelper;
import com.teo.ttasks.ui.base.Presenter;

import java.util.Date;

import io.realm.Realm;
import rx.android.schedulers.AndroidSchedulers;
import timber.log.Timber;

import static com.teo.ttasks.data.model.Task.STATUS_COMPLETED;
import static com.teo.ttasks.data.model.Task.STATUS_NEEDS_ACTION;

public class TaskDetailPresenter extends Presenter<TaskDetailView> {

    private final TasksHelper mTasksHelper;

    private Realm mRealm;

    private Task mTask;

    public TaskDetailPresenter(TasksHelper tasksHelper) {
        mTasksHelper = tasksHelper;
    }

    void getTask(String taskId) {
        mTasksHelper.getTask(taskId, mRealm)
                .subscribe(
                        task -> {
                            mTask = task;
                            final TaskDetailView view = view();
                            if (view != null) view.onTaskLoaded(task);
                        },
                        throwable -> {
                            Timber.e(throwable.toString());
                            final TaskDetailView view = view();
                            if (view != null) view.onTaskLoadError();
                        }
                );

    }

    void getTaskList(String taskListId) {
        mTasksHelper.getTaskList(taskListId, mRealm)
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
     *
     * @param taskListId the ID of the task list containing this task (required for the API call)
     */
    void updateCompletionStatus(String taskListId) {
        mRealm.executeTransaction(realm -> {
            boolean completed = mTask.getCompleted() != null;
            if (!completed) {
                mTask.setCompleted(new Date());
                mTask.setStatus(STATUS_COMPLETED);
                Timber.d("task was completed");
            } else {
                mTask.setCompleted(null);
                mTask.setStatus(STATUS_NEEDS_ACTION);
                Timber.d("task needs action");
            }
        });
        mTasksHelper.updateCompletionStatus(taskListId, mRealm.copyFromRealm(mTask))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        task -> {
                            final TaskDetailView view = view();
                            if (view != null) view.onTaskUpdated();
                        },
                        throwable -> {
                            final TaskDetailView view = view();
                            if (view != null) view.onTaskUpdateError();
                        }
                );
    }

    void deleteTask(String taskListId, String taskId) {
        mTasksHelper.deleteTask(taskListId, taskId)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        aVoid -> {
                            mRealm.executeTransaction(realm -> mTask.deleteFromRealm());
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
        mRealm = Realm.getDefaultInstance();
    }

    @Override
    public void unbindView(@NonNull TaskDetailView view) {
        super.unbindView(view);
        mRealm.close();
    }
}
