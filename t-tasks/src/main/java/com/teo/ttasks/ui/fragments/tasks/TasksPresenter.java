package com.teo.ttasks.ui.fragments.tasks;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.android.gms.auth.UserRecoverableAuthException;
import com.teo.ttasks.data.remote.TasksHelper;
import com.teo.ttasks.ui.base.Presenter;
import com.teo.ttasks.util.RxUtils;

import java.util.concurrent.atomic.AtomicInteger;

import io.realm.Realm;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import timber.log.Timber;

public class TasksPresenter extends Presenter<TasksView> {

    private final TasksHelper tasksHelper;

    private Realm realm;

    public TasksPresenter(TasksHelper tasksHelper) {
        this.tasksHelper = tasksHelper;
    }

    void getTasks(@Nullable String taskListId) {
        if (taskListId == null)
            return;
        {
            final TasksView view = view();
            if (view != null) view.onTasksLoading();
        }
        final Subscription subscription = tasksHelper.getTasks(taskListId, realm)
                .compose(RxUtils.getTaskItems())
                .subscribe(
                        // The Realm observable will not throw errors
                        tasks -> {
                            Timber.d("loaded %d tasks", tasks.size());
                            final TasksView view = view();
                            if (view != null) {
                                if (tasks.isEmpty()) view.showEmptyUi();
                                else view.showContentUi(tasks);
                            }
                        });
        unsubscribeOnUnbindView(subscription);
    }

    void refreshTasks(@Nullable String taskListId) {
        if (taskListId == null)
            return;
        final Subscription subscription = tasksHelper.refreshTasks(taskListId)
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
    void syncTasks(@Nullable String taskListId) {
        if (taskListId == null)
            return;
        // Keep track of the number of synced tasks
        AtomicInteger taskSyncCount = new AtomicInteger(0);
        final Subscription subscription = tasksHelper.syncTasks(taskListId)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        syncedTask -> {
                            // Sync successful for this task
                            realm.executeTransaction(realm -> {
                                syncedTask.setSynced(true);
                                // This task is not managed by Realm so it needs to be updated manually
                                realm.insertOrUpdate(syncedTask);
                            });
                            taskSyncCount.incrementAndGet();
                        },
                        throwable -> {
                            // Sync failed for some or all tasks
                            Timber.e(throwable.toString());
                            final TasksView view = view();
                            if (view != null) view.onSyncDone(taskSyncCount.get());
                        },
                        () -> {
                            // Syncing done
                            final TasksView view = view();
                            if (view != null) view.onSyncDone(taskSyncCount.get());
                        }
                );
        unsubscribeOnUnbindView(subscription);
    }

    @Override
    public void bindView(@NonNull TasksView view) {
        super.bindView(view);
        realm = Realm.getDefaultInstance();
    }

    @Override
    public void unbindView(@NonNull TasksView view) {
        super.unbindView(view);
        realm.close();
    }
}
