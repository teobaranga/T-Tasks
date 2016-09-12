package com.teo.ttasks.ui.activities.main;

import android.support.annotation.NonNull;
import android.support.v4.util.Pair;

import com.teo.ttasks.api.PeopleApi;
import com.teo.ttasks.data.local.PrefHelper;
import com.teo.ttasks.data.model.TTaskList;
import com.teo.ttasks.data.remote.TasksHelper;
import com.teo.ttasks.ui.base.Presenter;

import io.realm.Realm;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import timber.log.Timber;

public class MainActivityPresenter extends Presenter<MainView> {

    private final TasksHelper tasksHelper;
    private final PrefHelper prefHelper;
    private final PeopleApi peopleApi;

    private Realm mRealm;

    public MainActivityPresenter(TasksHelper tasksHelper, PrefHelper prefHelper, PeopleApi peopleApi) {
        this.tasksHelper = tasksHelper;
        this.prefHelper = prefHelper;
        this.peopleApi = peopleApi;
    }

    boolean isUserPresent() {
        return prefHelper.isUserPresent();
    }

    String getUserName() {
        return prefHelper.getUserName();
    }

    String getUserEmail() {
        return prefHelper.getUserEmail();
    }

    /**
     * Save the ID of the last accessed task list so that it can be displayed the next time the user opens the app
     *
     * @param taskListId task list identifier
     */
    void setLastAccessedTaskList(String taskListId) {
        prefHelper.setLastAccessedTaskList(taskListId);
    }

    /**
     * Load the account information for the currently signed in Google user.
     * Must be called after onConnected
     */
    void loadCurrentUser() {
        final Subscription subscription = peopleApi.getCurrentUserProfile()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        person -> {
                            final MainView view = view();
                            /**
                             * By default the profile url gives a 50x50 px image only,
                             * but we can replace the value with whatever dimension we want by replacing sz=X
                             */
                            String pictureUrl = person.image.url.split("\\?")[0];
                            if (!pictureUrl.equals(prefHelper.getUserPhoto())) {
                                prefHelper.setUserPhoto(pictureUrl);
                                if (view != null) view.onUserPicture(pictureUrl);
                            }

                            // Get cover picture
                            if (person.cover != null && person.cover.coverPhoto != null) {
                                String coverUrl = person.cover.coverPhoto.url;
                                if (!coverUrl.equals(prefHelper.getUserCover())) {
                                    prefHelper.setUserCover(coverUrl);
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
            if (prefHelper.getUserPhoto() != null)
                view.onUserPicture(prefHelper.getUserPhoto());
            if (prefHelper.getUserCover() != null)
                view.onUserCover(prefHelper.getUserCover());
        }
    }

    void getTaskLists() {
        final Subscription subscription = tasksHelper.getTaskLists(mRealm)
                .map(taskLists -> {
                    String currentTaskListId = prefHelper.getCurrentTaskListId();
                    // Find the index of the current task list
                    for (int i = 0; i < taskLists.size(); i++) {
                        TTaskList taskList = taskLists.get(i);
                        if (taskList.getId().equals(currentTaskListId))
                            return new Pair<>(taskLists, i);
                    }
                    return new Pair<>(taskLists, 0);
                })
                .subscribe(
                        taskListsIndexPair -> {
                            final MainView view = view();
                            if (view != null) view.onTaskListsLoaded(taskListsIndexPair.first, taskListsIndexPair.second);
                        },
                        throwable -> {
                            Timber.e(throwable.toString());
                            final MainView view = view();
                            if (view != null) view.onTaskListsLoadError();
                        });
        unsubscribeOnUnbindView(subscription);
    }

    void refreshTaskLists() {
        final Subscription subscription = tasksHelper.refreshTaskLists()
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
