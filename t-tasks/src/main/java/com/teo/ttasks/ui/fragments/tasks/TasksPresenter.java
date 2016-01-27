package com.teo.ttasks.ui.fragments.tasks;

import android.support.annotation.NonNull;

import com.teo.ttasks.data.remote.TasksHelper;
import com.teo.ttasks.data.local.RealmHelper;
import com.teo.ttasks.other.FinishAsyncJobSubscription;
import com.teo.ttasks.performance.AsyncJob;
import com.teo.ttasks.performance.AsyncJobsObserver;
import com.teo.ttasks.ui.base.Presenter;

import javax.inject.Inject;

import io.realm.Realm;
import rx.Scheduler;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import timber.log.Timber;

public class TasksPresenter extends Presenter<TasksView> {

    @NonNull
    private final Scheduler mScheduler;

    @NonNull
    private final TasksHelper mTasksHelper;

    @NonNull
    private final RealmHelper mRealmHelper;

    @NonNull
    private final AsyncJobsObserver mAsyncJobsObserver;

    @Inject
    public TasksPresenter(@NonNull Scheduler scheduler,
                          @NonNull TasksHelper tasksHelper,
                          @NonNull RealmHelper realmHelper,
                          @NonNull AsyncJobsObserver asyncJobsObserver) {
        mScheduler = scheduler;
        mTasksHelper = tasksHelper;
        mRealmHelper = realmHelper;
        mAsyncJobsObserver = asyncJobsObserver;
    }

    /**
     * Fetch the tasks for the given task list from Google
     * and update the local copies
     */
    public void reloadTasks(@NonNull String taskListId) {
        final AsyncJob asyncJob = mAsyncJobsObserver.asyncJobStarted("reloadTasks in TasksPresenter");
        final Subscription reloadSubscription = mTasksHelper.getTasks(taskListId)
                .subscribeOn(mScheduler)
                .observeOn(AndroidSchedulers.mainThread())
                .toList()
                .map(taskList -> {
                    // Sync online tasks with the offline database
                    Realm defaultRealm = Realm.getDefaultInstance();
                    defaultRealm.executeTransaction(realm -> {
                        mRealmHelper.clearTasks(realm, taskListId);
                        mRealmHelper.createTasks(realm, taskList, taskListId);
                    });
                    defaultRealm.close();
                    return taskList;
                })
                .subscribe(
                        tasks -> {
                            mAsyncJobsObserver.asyncJobFinished(asyncJob);
                        },
                        error -> {
                            Timber.e(error.toString());
                            // Tip: in Kotlin you can use ? to operate with nullable values.
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
        final Realm realm = Realm.getDefaultInstance();
        final Subscription subscription = mRealmHelper.loadTasks(realm, taskListId)
                .subscribe(
                        realmTasks -> {
                            final TasksView view = view();
                            if (view != null) {
                                if (realmTasks.isEmpty())
                                    view.showEmptyUi();
                                else
                                    view.showContentUi(realmTasks);
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
                        },
                        realm::close
                );
        // Prevent memory leak.
        unsubscribeOnUnbindView(subscription, new FinishAsyncJobSubscription(mAsyncJobsObserver, asyncJob));
    }

}
