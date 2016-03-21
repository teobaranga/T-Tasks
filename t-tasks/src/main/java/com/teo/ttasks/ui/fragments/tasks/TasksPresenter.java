package com.teo.ttasks.ui.fragments.tasks;

import android.support.annotation.NonNull;

import com.teo.ttasks.data.local.RealmHelper;
import com.teo.ttasks.data.model.Task;
import com.teo.ttasks.data.remote.TasksHelper;
import com.teo.ttasks.other.FinishAsyncJobSubscription;
import com.teo.ttasks.performance.AsyncJob;
import com.teo.ttasks.performance.AsyncJobsObserver;
import com.teo.ttasks.ui.base.Presenter;
import com.teo.ttasks.ui.items.TaskItem;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import rx.Observable;
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

    @NonNull
    private final AsyncJobsObserver mAsyncJobsObserver;

    @Inject
    public TasksPresenter(@NonNull TasksHelper tasksHelper,
                          @NonNull RealmHelper realmHelper,
                          @NonNull Scheduler realmScheduler,
                          @NonNull AsyncJobsObserver asyncJobsObserver) {
        mTasksHelper = tasksHelper;
        mRealmHelper = realmHelper;
        mRealmScheduler = realmScheduler;
        mAsyncJobsObserver = asyncJobsObserver;
    }

    /**
     * Fetch the tasks for the given task list from Google
     * and update the local copies
     */
    public void reloadTasks(@NonNull String taskListId) {
        final AsyncJob asyncJob = mAsyncJobsObserver.asyncJobStarted("reloadTasks in TasksPresenter");
        final Subscription reloadSubscription = mTasksHelper.getTasks(taskListId)
                .flatMap(taskList -> mRealmHelper.refreshTasks(taskList, taskListId))
                .flatMap(tasks -> {
                    List<TaskItem> taskItems = new ArrayList<>();
                    for (Task task : tasks)
                        taskItems.add(new TaskItem(task));
                    return Observable.just(taskItems);
                })
                .subscribeOn(mRealmScheduler)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        tasks -> {
                            final TasksView view = view();
                            if (view != null) {
                                if (tasks.isEmpty())
                                    view.showEmptyUi();
                                else
                                    view.showContentUi(tasks);
                            }
                            mAsyncJobsObserver.asyncJobFinished(asyncJob);
                        },
                        error -> {
                            Timber.e(error.toString());
                            final TasksView view = view();
                            if (view != null)
                                view.showErrorUi();
                            mAsyncJobsObserver.asyncJobFinished(asyncJob);
                        }
                );
        // Prevent memory leak.
        unsubscribeOnUnbindView(reloadSubscription, new FinishAsyncJobSubscription(mAsyncJobsObserver, asyncJob));
    }

    public void loadTasks(@NonNull String taskListId) {
        {
            final TasksView view = view();
            if (view != null) {
                view.showLoadingUi();
            }
        }
        final AsyncJob asyncJob = mAsyncJobsObserver.asyncJobStarted("loadTasks in TasksPresenter");
        final Subscription subscription = mRealmHelper.loadTasks(taskListId)
                .subscribeOn(mRealmScheduler)
                .flatMap(tasks -> {
                    List<TaskItem> taskItems = new ArrayList<>();
                    for (Task task : tasks)
                        taskItems.add(new TaskItem(task));
                    return Observable.just(taskItems);
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        tasks -> {
                            final TasksView view = view();
                            if (view != null) {
                                if (tasks.isEmpty())
                                    view.showEmptyUi();
                                else
                                    view.showContentUi(tasks);
                            }
                            mAsyncJobsObserver.asyncJobFinished(asyncJob);
                        },
                        error -> {
                            Timber.e("Error loading tasks from Realm");
                            Timber.e(error.toString());
                            final TasksView view = view();
                            if (view != null)
                                view.showErrorUi();
                            mAsyncJobsObserver.asyncJobFinished(asyncJob);
                        }
                );
        // Prevent memory leak.
        unsubscribeOnUnbindView(subscription, new FinishAsyncJobSubscription(mAsyncJobsObserver, asyncJob));
    }

}
