package com.teo.ttasks.ui.activities.main;

import android.support.annotation.NonNull;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.plus.Plus;
import com.google.android.gms.plus.model.people.Person;
import com.google.android.gms.plus.model.people.PersonBuffer;
import com.teo.ttasks.data.local.RealmHelper;
import com.teo.ttasks.data.remote.TasksHelper;
import com.teo.ttasks.ui.base.Presenter;

import javax.inject.Inject;

import io.realm.Realm;
import rx.Observable;
import rx.Scheduler;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import timber.log.Timber;

public class MainActivityPresenter extends Presenter<MainActivityView> {

    @NonNull
    private TasksHelper mTasksHelper;

    @NonNull
    private RealmHelper mRealmHelper;

    @NonNull
    private Scheduler mRealmScheduler;

    @Inject
    public MainActivityPresenter(@NonNull TasksHelper tasksHelper,
                                 @NonNull RealmHelper realmHelper,
                                 @NonNull Scheduler realmScheduler) {
        mTasksHelper = tasksHelper;
        mRealmHelper = realmHelper;
        mRealmScheduler = realmScheduler;
    }

    /**
     * Load the account information for the currently signed in Google user.
     * Must be called after onConnected
     */
    public void loadCurrentUser(@NonNull GoogleApiClient googleApiClient) {
        Observable.just(Plus.PeopleApi.load(googleApiClient, "me"))
                .subscribeOn(Schedulers.io())
                .flatMap(loadPeopleResultPendingResult -> Observable.just(loadPeopleResultPendingResult.await()))
                .subscribe(
                        loadPeopleResult -> {
                            if (loadPeopleResult.getStatus().isSuccess()) {
                                PersonBuffer personBuffer = loadPeopleResult.getPersonBuffer();
                                try {
                                    Person currentPerson = personBuffer.get(0);
                                    if (currentPerson != null) {
                                        final MainActivityView view = view();
                                        if (view != null)
                                            view.onUserLoaded(currentPerson);
                                    }
                                    // TODO: 2015-12-29 handle null person
                                } finally {
                                    personBuffer.release();
                                }
                            } else {
                                // TODO: 2015-12-29 Show error UI
                                // TODO: 2015-12-29 Test for errors (getResolution())
                                Timber.e("Error getting me: %s", loadPeopleResult.getStatus());
                            }
                        },
                        error -> Timber.e(error.toString())
                );
    }

    /**
     * Fetch the task lists from Google
     * and update the local copies
     */
    public void reloadTaskLists() {
        // TODO: 2015-12-29 Show loading UI
        System.out.println("Reloading task lists");
        mTasksHelper.getTaskLists()
                .subscribeOn(Schedulers.io())
                .map(taskList -> {
                    // Load the tasks from each task list
                    mTasksHelper.getTasks(taskList.getId())
                            .subscribeOn(mRealmScheduler)
                            .map(tasks -> {
                                // Sync online tasks with the offline database
                                Realm defaultRealm = Realm.getDefaultInstance();
                                defaultRealm.executeTransaction(realm -> {
                                    mRealmHelper.clearTasks(taskList.getId(), realm);
                                    mRealmHelper.createTasks(tasks, taskList.getId(), realm);
                                });
                                defaultRealm.close();
                                return taskList;
                            })
                            .subscribe();
                    return taskList;
                })
                .toList()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        taskLists -> {
                            // TODO: 2015-12-29 Show empty UI
                            if (taskLists == null)
                                return;
                            final MainActivityView view = view();
                            if (view != null)
                                view.onTaskListsLoaded(taskLists);
                        },
                        error -> {
                            Timber.e("Error getting task lists");
                            Timber.e(error.toString());
                        }
                );
    }

    public void loadTaskLists() {
        mRealmHelper.loadTaskLists()
                .subscribe(
                        taskLists -> {
                            // TODO: 2015-12-29 Show empty UI
                            if (taskLists == null)
                                return;
                            final MainActivityView view = view();
                            if (view != null)
                                view.onCachedTaskListsLoaded(taskLists);
                        },
                        error -> Timber.e(error.toString()));
    }

}
