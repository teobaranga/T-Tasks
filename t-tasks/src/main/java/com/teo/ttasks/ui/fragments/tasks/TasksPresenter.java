package com.teo.ttasks.ui.fragments.tasks;

import android.support.annotation.NonNull;

import com.google.android.gms.auth.UserRecoverableAuthException;
import com.teo.ttasks.data.remote.TasksHelper;
import com.teo.ttasks.ui.base.Presenter;
import com.teo.ttasks.util.RxUtil;

import java.util.concurrent.atomic.AtomicInteger;

import io.realm.Realm;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import timber.log.Timber;

public class TasksPresenter extends Presenter<TasksView> {

    private final TasksHelper mTasksHelper;

    private Realm mRealm;

    public TasksPresenter(TasksHelper tasksHelper) {
        mTasksHelper = tasksHelper;
    }

    void getTasks(String taskListId) {
        final Subscription subscription = mTasksHelper.getTasks(taskListId, mRealm)
                .compose(RxUtil.getTaskItems())
                .subscribe(
                        tasks -> {
                            Timber.d("loaded %d tasks", tasks.size());
                            final TasksView view = view();
                            if (view != null) {
                                if (tasks.isEmpty()) view.showEmptyUi();
                                else view.showContentUi(tasks);
                            }
                        },
                        throwable -> {
                            Timber.e(throwable.toString());
                            // TODO: 2016-07-12 error
                        });
        unsubscribeOnUnbindView(subscription);
    }

    void refreshTasks(String taskListId) {
        final Subscription subscription = mTasksHelper.refreshTasks(taskListId)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        tasksResponse -> { /* ignored since onCompleted does the job, even when the tasks have not been updated */ },
                        throwable -> {
                            Timber.e(throwable.toString());
                            final TasksView view = view();
                            if (view != null) {
                                if (throwable.getCause() instanceof UserRecoverableAuthException)
                                    view.onTasksLoadError(((UserRecoverableAuthException) throwable.getCause()).getIntent());
                                else
                                    view.onTasksLoadError(null);
                            }
                        },
                        () -> {
                            final TasksView view = view();
                            if (view != null) view.onRefreshDone();
                        }
                );
        unsubscribeOnUnbindView(subscription);
    }

    /**
     * Synchronize the local tasks from the specified task list.
     *
     * @param taskListId task list identifier
     */
    void syncTasks(String taskListId) {
        // Keep track of the number of synced tasks
        AtomicInteger taskSyncCount = new AtomicInteger(0);
        final Subscription subscription = mTasksHelper.syncTasks(taskListId)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        syncedTask -> {
                            // Sync successful for this task
                            mRealm.executeTransaction(realm -> syncedTask.setSynced(true));
                            taskSyncCount.incrementAndGet();
                        },
                        throwable -> {
                            // Sync failed for some or all tasks
                            Timber.e(throwable.toString());
                        },
                        () -> {
                            // Syncing done
                            if (taskSyncCount.get() != 0) {
                                final TasksView view = view();
                                if (view != null) view.onSyncDone(taskSyncCount.get());
                            }
                        }
                );
        unsubscribeOnUnbindView(subscription);
    }

    @Override
    public void bindView(@NonNull TasksView view) {
        super.bindView(view);
        mRealm = Realm.getDefaultInstance();
    }

    @Override
    public void unbindView(@NonNull TasksView view) {
        super.unbindView(view);
        mRealm.close();
    }
}
