package com.teo.ttasks.ui.fragments.tasks;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.teo.ttasks.data.local.PrefHelper;
import com.teo.ttasks.data.model.TTask;
import com.teo.ttasks.data.remote.TasksHelper;
import com.teo.ttasks.ui.base.Presenter;
import com.teo.ttasks.util.RxUtils;

import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

import io.realm.Realm;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import timber.log.Timber;

public class TasksPresenter extends Presenter<TasksView> {

    private final TasksHelper tasksHelper;
    private final PrefHelper prefHelper;

    private Subscription tasksSubscription;

    @RxUtils.SortingMode
    private int sortingMode;

    private boolean listeners;

    Realm realm;

    public TasksPresenter(TasksHelper tasksHelper, PrefHelper prefHelper) {
        this.tasksHelper = tasksHelper;
        this.prefHelper = prefHelper;
        sortingMode = prefHelper.getSortMode();
    }

    /**
     * Load the tasks associated with the provided task list from the local database.
     *
     * @param taskListId task list identifier
     */
    void getTasks(@Nullable String taskListId) {
        if (taskListId == null)
            return;
        final AtomicInteger taskCount = new AtomicInteger();
        // Since Realm observables do not complete, this subscription must be recreated every time
        if (tasksSubscription != null && !tasksSubscription.isUnsubscribed())
            tasksSubscription.unsubscribe();
        {
            final TasksView view = view();
            if (view != null) view.onTasksLoading();
        }
        final DatabaseReference tasks = FirebaseDatabase.getInstance().getReference("tasks");
        tasksSubscription = tasksHelper.getTasks(taskListId, realm)
                .doOnNext(tTasks -> {
                    if (!listeners) {
                        listeners = true;
                        for (TTask tTask : tTasks) {
                            // TODO: 2016-10-01 remove this at sign out
                            tasks.child(tTask.getId()).child("reminder").addValueEventListener(new ValueEventListener() {
                                @Override public void onDataChange(DataSnapshot dataSnapshot) {
                                    if (dataSnapshot.exists()) {
                                        Timber.d("restoring reminder for %s", tTask.getId());
                                        final Long reminder = dataSnapshot.getValue(Long.class);
                                        realm.executeTransaction(realm -> tTask.setReminder(new Date(reminder)));
                                    }
                                }

                                @Override public void onCancelled(DatabaseError databaseError) {
                                    // Do nothing
                                }
                            });
                        }
                    }
                })
                .compose(RxUtils.getTaskItems(sortingMode))
                .subscribe(
                        // The Realm observable will not throw errors
                        taskListObservable -> {
                            if (taskListObservable.getKey()) {
                                // Active tasks
                                taskListObservable
                                        .subscribe(
                                                taskItems -> {
                                                    Timber.d("loaded %d active tasks", taskItems.size());
                                                    final TasksView view = view();
                                                    if (view != null) {
                                                        view.onActiveTasksLoaded(taskItems);
                                                        if (!taskItems.isEmpty()) taskCount.addAndGet(taskItems.size());
                                                    }
                                                },
                                                throwable -> Timber.e(throwable.toString())
                                        );
                            } else {
                                // Completed tasks
                                taskListObservable
                                        .subscribe(
                                                taskItems -> {
                                                    Timber.d("loaded %d completed tasks", taskItems.size());
                                                    final TasksView view = view();
                                                    if (view != null) {
                                                        // Show completed tasks
                                                        view.onCompletedTasksLoaded(taskItems);
                                                        if (!taskItems.isEmpty()) taskCount.addAndGet(taskItems.size());

                                                        if (taskCount.get() == 0) {
                                                            view.onTasksEmpty();
                                                        } else {
                                                            view.onTasksLoaded();
                                                            taskCount.set(0);
                                                        }
                                                    }
                                                },
                                                throwable -> Timber.e(throwable.toString())
                                        );
                            }
                        });
        unsubscribeOnUnbindView(tasksSubscription);
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
                            // Sync failed for at least one task, will retry on next refresh
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

    boolean getShowCompleted() {
        return prefHelper.getShowCompleted();
    }

    void setShowCompleted(boolean showCompleted) {
        prefHelper.setShowCompleted(showCompleted);
    }

    /**
     * Switch the sorting mode.
     *
     * @param sortingMode the new sorting mode
     * @return true if the new sorting mode is different, false otherwise
     */
    boolean switchSortMode(@RxUtils.SortingMode int sortingMode) {
        if (sortingMode != this.sortingMode) {
            this.sortingMode = sortingMode;
            prefHelper.setSortMode(sortingMode);
            return true;
        }
        return false;
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
