package com.teo.ttasks.ui.fragments.tasks;

import android.support.annotation.NonNull;

import com.teo.ttasks.data.local.RealmHelper;
import com.teo.ttasks.data.remote.TasksHelper;
import com.teo.ttasks.ui.base.Presenter;
import com.teo.ttasks.util.RxUtil;

import javax.inject.Inject;

import rx.Scheduler;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import timber.log.Timber;

public class TasksPresenter extends Presenter<TasksView> {

    @NonNull
    private final Scheduler mRealmScheduler;

    @NonNull
    private final TasksHelper mTasksHelper;

    @NonNull
    private final RealmHelper mRealmHelper;

    @Inject
    public TasksPresenter(@NonNull TasksHelper tasksHelper,
                          @NonNull RealmHelper realmHelper,
                          @NonNull Scheduler realmScheduler) {
        mTasksHelper = tasksHelper;
        mRealmHelper = realmHelper;
        mRealmScheduler = realmScheduler;
    }

    /**
     * Fetch the tasks for the given task list from Google
     * and update the local copies
     */
    public void reloadTasks(@NonNull String taskListId) {
        final Subscription reloadSubscription = mTasksHelper.getTasks(taskListId)
                .flatMap(taskList -> mRealmHelper.refreshTasks(taskList, taskListId))
                .compose(RxUtil.getTaskItems())
                .subscribeOn(mRealmScheduler)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        tasks -> {
                            final TasksView view = view();
                            if (view != null) {
                                if (tasks.isEmpty()) view.showEmptyUi();
                                else view.showContentUi(tasks);
                            }
                        },
                        error -> {
                            Timber.e(error.toString());
                            final TasksView view = view();
                            if (view != null) view.showErrorUi();
                        }
                );
        // Prevent memory leak.
        unsubscribeOnUnbindView(reloadSubscription);
    }

    public void loadTasks(@NonNull String taskListId) {
        {
            final TasksView view = view();
            if (view != null) view.showLoadingUi();
        }
        final Subscription subscription = mRealmHelper.loadTasks(taskListId)
                .compose(RxUtil.getTaskItems())
                .subscribeOn(mRealmScheduler)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        tasks -> {
                            final TasksView view = view();
                            if (view != null)
                                if (tasks.isEmpty()) view.showEmptyUi();
                                else view.showContentUi(tasks);
                        },
                        error -> {
                            Timber.e("Error loading tasks from Realm: %s", error.toString());
                            final TasksView view = view();
                            if (view != null) view.showErrorUi();
                        }
                );
        // Prevent memory leak.
        unsubscribeOnUnbindView(subscription);
    }

}
