package com.teo.ttasks.ui.activities.main;

import android.support.annotation.NonNull;

import com.teo.ttasks.api.PeopleApi;
import com.teo.ttasks.data.local.PrefHelper;
import com.teo.ttasks.data.remote.TasksHelper;
import com.teo.ttasks.ui.base.Presenter;

import io.realm.Realm;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import timber.log.Timber;

public class MainActivityPresenter extends Presenter<MainView> {

    private final TasksHelper mTasksHelper;
    private final PrefHelper mPrefHelper;
    private final PeopleApi mPeopleApi;

    private Realm mRealm;

    public MainActivityPresenter(TasksHelper tasksHelper, PrefHelper prefHelper, PeopleApi peopleApi) {
        mTasksHelper = tasksHelper;
        mPrefHelper = prefHelper;
        mPeopleApi = peopleApi;
    }

    /**
     * Load the account information for the currently signed in Google user.
     * Must be called after onConnected
     */
    void loadCurrentUser() {
        final Subscription subscription = mPeopleApi.getCurrentUserProfile()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        person -> {
                            final MainView view = view();
                            /**
                             * By default the profile url gives a 50x50 px image only,
                             * but we can replace the value with whatever dimension we want by replacing sz=X
                             */
                            String pictureUrl = person.image.url.split("\\?")[0];
                            if (!pictureUrl.equals(mPrefHelper.getUserPhoto())) {
                                mPrefHelper.setUserPhoto(pictureUrl);
                                if (view != null) view.onUserPicture(pictureUrl);
                            }

                            // Get cover picture
                            if (person.cover != null && person.cover.coverPhoto != null) {
                                String coverUrl = person.cover.coverPhoto.url;
                                if (!coverUrl.equals(mPrefHelper.getUserCover())) {
                                    mPrefHelper.setUserCover(coverUrl);
                                    if (view != null) view.onUserCover(coverUrl);
                                }
                            }
                        },
                        throwable -> Timber.e(throwable.toString())
                );
        unsubscribeOnUnbindView(subscription);
    }

    void loadUserPictures() {
        final MainView view = view();
        if (view != null) {
            if (mPrefHelper.getUserPhoto() != null)
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
