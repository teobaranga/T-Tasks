package com.teo.ttasks.data.local;

import android.support.annotation.NonNull;

import com.teo.ttasks.data.model.Task;
import com.teo.ttasks.data.model.TaskList;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.realm.Realm;
import io.realm.RealmResults;
import rx.Observable;

import static com.teo.ttasks.data.model.Task.TASK_LIST_ID;

@Singleton
public class RealmHelper {

    @Inject
    public RealmHelper() {
    }

    /**
     * Loads all the task lists from the local Realm database.
     * They are associated with the Realm instance of the subscriber thread.
     *
     * @return an observable emitting the task lists found
     */
    @NonNull
    public Observable<RealmResults<TaskList>> getTaskLists() {
        return Observable.defer(() -> Realm.getDefaultInstance().where(TaskList.class).findAll().asObservable().first());
    }

    /**
     * Loads the tasks belonging to the provided task list from the local Realm database.
     * They are associated with the Realm instance of the subscriber thread.
     *
     * @param taskListId the id of the task list
     * @return an observable emitting the tasks found
     */
    @NonNull
    public Observable<RealmResults<Task>> getTasks(@NonNull String taskListId) {
        return Observable.defer(() ->
                Realm.getDefaultInstance().where(Task.class).equalTo(TASK_LIST_ID, taskListId).findAll().asObservable().first());
    }

    /**
     * Refresh the tasks from the specified task list
     *
     * @param newTasks
     * @param taskListId
     * @return
     */
    @NonNull
    public Observable<RealmResults<Task>> refreshTasks(@NonNull List<com.google.api.services.tasks.model.Task> newTasks,
                                                       @NonNull String taskListId) {
        return Observable.defer(() -> {
            final Realm realm = Realm.getDefaultInstance();
            return getTasks(taskListId)
                    .doOnUnsubscribe(realm::close)
                    .doOnCompleted(realm::close)
                    .doOnError(throwable -> realm.close())
                    .map(tasks -> {
                        // Delete all cached tasks from the task list
                        realm.executeTransaction(r -> tasks.deleteAllFromRealm());
                        return null;
                    })
                    .switchMap(ignored -> Observable.just(newTasks))
                    .map(taskList -> {
                        // Cache the new Google tasks
                        realm.executeTransaction(r -> {
                            for (com.google.api.services.tasks.model.Task task : taskList)
                                Task.create(task, taskListId, realm);
                        });
                        return null;
                    })
                    .switchMap(ignored -> getTasks(taskListId));
        });
    }

    @NonNull
    public Observable<RealmResults<TaskList>> refreshTaskLists(@NonNull List<com.google.api.services.tasks.model.TaskList> newTaskLists) {
        return Observable.defer(() -> {
            final Realm realm = Realm.getDefaultInstance();
            return getTaskLists()
                    .doOnUnsubscribe(realm::close)
                    .doOnCompleted(realm::close)
                    .doOnError(throwable -> realm.close())
                    .map(cachedTaskLists -> {
                        // Delete all cached task lists
                        realm.executeTransaction(r -> cachedTaskLists.deleteAllFromRealm());
                        return null;
                    })
                    .switchMap(ignored -> Observable.just(newTaskLists))
                    .map(taskLists -> {
                        // Cache the new Google task lists
                        realm.executeTransaction(r -> {
                            for (com.google.api.services.tasks.model.TaskList taskList : taskLists)
                                realm.createOrUpdateObjectFromJson(TaskList.class, taskList.toString());
                        });
                        return null;
                    })
                    .switchMap(ignored -> getTaskLists());
        });
    }
}
