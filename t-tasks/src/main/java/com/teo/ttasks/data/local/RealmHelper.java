package com.teo.ttasks.data.local;

import android.support.annotation.NonNull;

import com.teo.ttasks.data.model.Task;
import com.teo.ttasks.data.model.TaskList;

import java.util.List;

import javax.inject.Inject;

import io.realm.Realm;
import io.realm.RealmResults;
import rx.Observable;

import static com.teo.ttasks.data.model.Task.TASK_LIST_ID;

public class RealmHelper {

    @Inject
    public RealmHelper() {
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

    /**
     * Remove all the tasks from the task list with the provided ID
     */
    public void clearTasks(@NonNull Realm realm, @NonNull String taskListId) {
        realm.where(Task.class).equalTo(TASK_LIST_ID, taskListId).findAll().clear();
    }

    /**
     * Create local copies from the provided list of Google tasks
     */
    public void createTasks(@NonNull Realm realm,
                            @NonNull List<com.google.api.services.tasks.model.Task> tasks,
                            @NonNull String taskListId) {
        for (com.google.api.services.tasks.model.Task task : tasks) {
            Task t = realm.createOrUpdateObjectFromJson(Task.class, task.toString());
            Task.fixDates(t);
            t.setTaskListId(taskListId);
        }
    }
}
