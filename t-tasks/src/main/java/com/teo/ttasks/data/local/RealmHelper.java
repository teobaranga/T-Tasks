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

    @NonNull
    private Realm mRealm;

    @Inject
    public RealmHelper(@NonNull Realm realm) {
        mRealm = realm;
    }

    /**
     * Get all the task lists stored offline
     */
    @NonNull
    public Observable<RealmResults<TaskList>> loadTaskLists() {
        return mRealm.allObjects(TaskList.class).<TaskList>asObservable();
    }

    @NonNull
    public Observable<RealmResults<Task>> loadTasks(@NonNull String taskListId) {
        return mRealm.where(Task.class).equalTo(TASK_LIST_ID, taskListId).findAll().asObservable();
    }

    /**
     * Remove all the tasks from the task list with the provided ID
     */
    public void clearTasks(@NonNull String taskListId) {
        mRealm.where(Task.class).equalTo(TASK_LIST_ID, taskListId).findAll().clear();
    }

    /**
     * Create local copies from the provided list of Google tasks
     */
    public void createTasks(@NonNull List<com.google.api.services.tasks.model.Task> tasks,
                            @NonNull String taskListId) {
        for (com.google.api.services.tasks.model.Task task : tasks) {
            Task t = mRealm.createOrUpdateObjectFromJson(Task.class, task.toString());
            Task.fixDates(t);
            t.setTaskListId(taskListId);
        }
    }
}
