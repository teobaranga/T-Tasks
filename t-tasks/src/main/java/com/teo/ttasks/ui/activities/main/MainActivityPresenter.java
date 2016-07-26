package com.teo.ttasks.ui.activities.main;

import android.support.annotation.NonNull;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.plus.Plus;
import com.google.android.gms.plus.model.people.Person;
import com.google.android.gms.plus.model.people.PersonBuffer;
import com.teo.ttasks.data.local.PrefHelper;
import com.teo.ttasks.data.remote.TasksHelper;
import com.teo.ttasks.ui.base.Presenter;

import io.realm.Realm;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import timber.log.Timber;

public class MainActivityPresenter extends Presenter<MainView> {

    private final TasksHelper mTasksHelper;
    private final PrefHelper mPrefHelper;

    private Realm mRealm;

    public MainActivityPresenter(TasksHelper tasksHelper, PrefHelper prefHelper) {
        mTasksHelper = tasksHelper;
        mPrefHelper = prefHelper;
    }

    /**
     * Load the account information for the currently signed in Google user.
     * Must be called after onConnected
     */
    void loadCurrentUser(@NonNull GoogleApiClient googleApiClient) {
        final Subscription subscription = Observable.just(Plus.PeopleApi.load(googleApiClient, "me"))
                .flatMap(loadPeopleResultPendingResult -> Observable.just(loadPeopleResultPendingResult.await()))
                .flatMap(loadPeopleResult -> {
                    if (loadPeopleResult.getStatus().isSuccess()) {
                        PersonBuffer personBuffer = loadPeopleResult.getPersonBuffer();
                        try {
                            Person currentPerson = personBuffer.get(0);
                            // TODO: 2015-12-29 handle null person
                            return currentPerson != null ? Observable.just(currentPerson) : Observable.empty();
                        } finally {
                            personBuffer.release();
                        }
                    } else {
                        // TODO: 2015-12-29 Show error UI
                        // TODO: 2015-12-29 Test for errors (getResolution())
                        return Observable.error(new Throwable(loadPeopleResult.getStatus().toString()));
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        person -> {
                            final MainView view = view();
                            /**
                             * By default the profile url gives a 50x50 px image only,
                             * but we can replace the value with whatever dimension we want by replacing sz=X
                             */
                            String pictureUrl = person.getImage().getUrl();
                            // Requesting a size of 400x400
                            pictureUrl = pictureUrl.substring(0, pictureUrl.length() - 2) + "400";
                            if (!pictureUrl.equals(mPrefHelper.getUserPhoto())) {
                                mPrefHelper.setUserPhoto(pictureUrl);
                                if (view != null) view.onUserPicture(pictureUrl);
                            }

                            // Get cover picture
                            if (person.hasCover()) {
                                String coverUrl = person.getCover().getCoverPhoto().getUrl();
                                if (!coverUrl.equals(mPrefHelper.getUserCover())) {
                                    mPrefHelper.setUserCover(coverUrl);
                                    if (view != null) view.onUserCover(coverUrl);
                                }
                            }
                        },
                        error -> Timber.e(error.toString())
                );
        unsubscribeOnUnbindView(subscription);
    }

    void loadUserPictures() {
        final MainView view = view();
        if (view != null) {
            view.onUserPicture(mPrefHelper.getUserPhoto());
            if (mPrefHelper.getUserCover() != null)
                view.onUserCover(mPrefHelper.getUserCover());
        }
    }

    void getTaskLists() {
        final Subscription subscription = mTasksHelper.getTaskLists(mRealm)
                .subscribe(
                        taskLists -> {
                            Timber.d("loaded %d task lists", taskLists.size());
                            final MainView view = view();
                            if (view != null) view.onTaskListsLoaded(taskLists);
                        },
                        throwable -> {
                            Timber.e(throwable.toString());
                            final MainView view = view();
                            if (view != null) view.onTaskListsLoadError();
                        });
        unsubscribeOnUnbindView(subscription);
    }

    void refreshTaskLists() {
        final Subscription subscription = mTasksHelper.refreshTaskLists()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        taskListsResponse -> {
                        },
                        throwable -> {
                            Timber.e(throwable.toString());
                        }
                );
        unsubscribeOnUnbindView(subscription);
    }

    @Override
    public void bindView(@NonNull MainView view) {
        super.bindView(view);
        mRealm = Realm.getDefaultInstance();
    }

    @Override
    public void unbindView(@NonNull MainView view) {
        super.unbindView(view);
        mRealm.close();
    }
}
