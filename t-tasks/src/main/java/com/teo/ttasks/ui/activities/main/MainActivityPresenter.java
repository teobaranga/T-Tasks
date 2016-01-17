package com.teo.ttasks.ui.activities.main;

import android.support.annotation.NonNull;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.plus.Plus;
import com.google.android.gms.plus.model.people.Person;
import com.google.android.gms.plus.model.people.PersonBuffer;
import com.teo.ttasks.data.RealmModel;
import com.teo.ttasks.data.TasksModel;

import javax.inject.Inject;

import io.realm.Realm;
import rx.Observable;
import rx.schedulers.Schedulers;
import timber.log.Timber;

public class MainActivityPresenter {

    @Inject
    TasksModel mTasksModel;

    @NonNull
    private RealmModel mRealmModel;

    private MainActivity mMainActivity;

    @Inject
    public MainActivityPresenter(MainActivity mainActivity, @NonNull RealmModel realmModel) {
        mMainActivity = mainActivity;
        mRealmModel = realmModel;
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
                                    if (currentPerson != null)
                                        mMainActivity.onUserLoaded(currentPerson);
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
                        error -> {
                            Timber.e(error.toString());
                        }
                );
    }

    /**
     * Fetch the task lists from Google
     * and update the local copies
     */
    public void reloadTaskLists() {
        // TODO: 2015-12-29 Show loading UI
        mTasksModel.getTaskLists()
                .subscribeOn(Schedulers.io())
                .toList()
                .subscribe(
                        taskLists -> {
                            // TODO: 2015-12-29 Show empty UI
                            if (taskLists == null)
                                return;
                            mMainActivity.onTaskListsLoaded(taskLists);
                        },
                        error -> {
                            Timber.e("Error getting task lists");
                            Timber.e(error.toString());
                        }
                );
    }

    public void loadTaskLists() {
        final Realm realm = Realm.getDefaultInstance();
        mRealmModel.loadTaskLists(realm)
                .subscribe(
                        taskLists -> {
                            // TODO: 2015-12-29 Show empty UI
                            if (taskLists == null)
                                return;
                            mMainActivity.onCachedTaskListsLoaded(taskLists);
                        },
                        error -> Timber.e(error.toString()),
                        realm::close);
    }

}
