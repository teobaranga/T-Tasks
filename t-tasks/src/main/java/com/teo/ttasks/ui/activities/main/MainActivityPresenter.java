package com.teo.ttasks.ui.activities.main;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.plus.Plus;
import com.google.android.gms.plus.model.people.Person;
import com.google.android.gms.plus.model.people.PersonBuffer;
import com.teo.ttasks.data.local.RealmHelper;
import com.teo.ttasks.data.model.Task;
import com.teo.ttasks.data.model.TaskList;
import com.teo.ttasks.data.remote.TasksHelper;
import com.teo.ttasks.ui.base.Presenter;

import java.util.List;

import javax.inject.Inject;

import io.realm.Realm;
import io.realm.RealmQuery;
import rx.Observable;
import rx.Scheduler;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import timber.log.Timber;

import static com.teo.ttasks.data.model.Task.TASK_LIST_ID;

public class MainActivityPresenter extends Presenter<MainActivityView> {

    @Inject
    @Nullable
    public TasksHelper mTasksHelper;

    @NonNull
    private RealmHelper mRealmHelper;

    @NonNull
    private Scheduler mRealmScheduler;

    @Inject
    public MainActivityPresenter(@NonNull RealmHelper realmHelper, @NonNull Scheduler realmScheduler) {
        mRealmHelper = realmHelper;
        mRealmScheduler = realmScheduler;
    }

    /**
     * Load the account information for the currently signed in Google user.
     * Must be called after onConnected
     */
    public void loadCurrentUser(@NonNull GoogleApiClient googleApiClient) {
        final Subscription subscription = Observable.just(Plus.PeopleApi.load(googleApiClient, "me"))
                .subscribeOn(Schedulers.io())
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
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        person -> {
                            final MainActivityView view = view();
                            /**
                             * By default the profile url gives a 50x50 px image only,
                             * but we can replace the value with whatever dimension we want by replacing sz=X
                             */
                            String pictureUrl = person.getImage().getUrl();
                            // Requesting a size of 400x400
                            pictureUrl = pictureUrl.substring(0, pictureUrl.length() - 2) + "400";
                            if (view != null) view.onUserPicture(pictureUrl);

                            // Get cover picture
                            if (person.hasCover()) {
                                Timber.d("got cover");
                                String coverUrl = person.getCover().getCoverPhoto().getUrl();
                                if (coverUrl != null && view != null) view.onUserCover(coverUrl);
                            }
                        },
                        error -> Timber.e(error.toString())
                );
        unsubscribeOnUnbindView(subscription);
    }


    /**
     * Fetch the task lists from Google and update the local copies, if requested
     */
    public void getTaskLists(boolean refresh) {
        Observable<List<TaskList>> taskListObservable;
        if (refresh && mTasksHelper != null) {
            // Get task lists from Google
            taskListObservable = mTasksHelper.getTaskLists().subscribeOn(mRealmScheduler)
                    .flatMap(taskList -> mRealmHelper.refreshTaskLists(taskList))
                    .map(taskLists -> {
                        // Prune tasks - delete those that are associated with missing task lists
                        Realm realm = Realm.getDefaultInstance();
                        RealmQuery<Task> tasksToDelete = realm.where(Task.class);
                        for (TaskList taskList : taskLists)
                            tasksToDelete.notEqualTo(TASK_LIST_ID, taskList.getId());
                        tasksToDelete.findAll().deleteAllFromRealm();

                        return realm.copyFromRealm(taskLists);
                    });
        } else {
            // Load only the cached task lists
            taskListObservable = mRealmHelper.getTaskLists().subscribeOn(mRealmScheduler)
                    .map(taskLists -> Realm.getDefaultInstance().copyFromRealm(taskLists));
        }
        final Subscription subscription = taskListObservable
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
                        error -> Timber.e(error.toString()));

        unsubscribeOnUnbindView(subscription);
    }

}
