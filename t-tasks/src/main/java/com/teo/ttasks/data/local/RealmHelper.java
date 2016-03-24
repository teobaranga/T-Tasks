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
    public Observable<RealmResults<TaskList>> loadTaskLists() {
        return Observable.defer(() -> Realm.getDefaultInstance().allObjects(TaskList.class).asObservable().first());
    }

    /**
     * Loads the tasks belonging to the provided task list from the local Realm database.
     * They are associated with the Realm instance of the subscriber thread.
     *
     * @param taskListId the id of the task list
     * @return an observable emitting the tasks found
     */
    @NonNull
    public Observable<RealmResults<Task>> loadTasks(@NonNull String taskListId) {
        return Observable.defer(() ->
                Realm.getDefaultInstance().where(Task.class).equalTo(TASK_LIST_ID, taskListId).findAll().asObservable().first());
    }

    /**
     * Remove all the tasks from the task list with the provided ID
     */
    public void clearTasks(@NonNull String taskListId, @NonNull Realm realm) {
        realm.where(Task.class).equalTo(TASK_LIST_ID, taskListId).findAll().clear();
    }

    /**
     * Create local copies from the provided list of Google tasks
     */
    public void createTasks(@NonNull List<com.google.api.services.tasks.model.Task> tasks,
                            @NonNull String taskListId,
                            @NonNull Realm realm) {
        for (com.google.api.services.tasks.model.Task task : tasks)
            Task.create(task, taskListId, realm);
    }

    public Observable<RealmResults<Task>> refreshTasks(@NonNull List<com.google.api.services.tasks.model.Task> newTaskList,
                                                       @NonNull String taskListId) {
        return Observable.defer(() -> {
            final Realm realm = Realm.getDefaultInstance();
            return loadTasks(taskListId)
                    .doOnUnsubscribe(realm::close)
                    .doOnCompleted(realm::close)
                    .doOnError(throwable -> realm.close())
                    .map(tasks -> {
                        realm.executeTransaction(r -> tasks.clear());
                        return null;
                    })
                    .switchMap(ignored -> Observable.just(newTaskList))
                    .map(taskList -> {
                        realm.executeTransaction(r -> createTasks(taskList, taskListId, realm));
                        return null;
                    })
                    .switchMap(ignored -> loadTasks(taskListId));
        });
    }
}
