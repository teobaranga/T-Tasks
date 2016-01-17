package com.teo.ttasks.data;

import android.support.annotation.NonNull;

import com.teo.ttasks.data.model.Task;
import com.teo.ttasks.data.model.TaskList;

import javax.inject.Inject;

import io.realm.Realm;
import io.realm.RealmResults;
import rx.Observable;

import static com.teo.ttasks.data.model.Task.TASK_LIST_ID;

public class RealmModel {

    @Inject
    public RealmModel() {
    }

    /**
     * Get all the task lists stored offline
     */
    @NonNull
    public Observable<RealmResults<TaskList>> loadTaskLists(@NonNull Realm realm) {
        return realm.allObjects(TaskList.class).<TaskList>asObservable();
    }

    @NonNull
    public Observable<RealmResults<Task>> loadTasks(@NonNull Realm realm, @NonNull String taskListId) {
        return realm.where(Task.class).equalTo(TASK_LIST_ID, taskListId).findAll().asObservable();
    }

    public void clearTasks(@NonNull Realm realm, @NonNull String taskListId) {
        realm.where(Task.class).equalTo(TASK_LIST_ID, taskListId).findAll().clear();
    }
}
