package com.teo.ttasks.data.remote;

import com.teo.ttasks.api.TasksApi;
import com.teo.ttasks.api.entities.TaskListsResponse;
import com.teo.ttasks.api.entities.TasksResponse;
import com.teo.ttasks.data.local.PrefHelper;
import com.teo.ttasks.data.model.Task;
import com.teo.ttasks.data.model.TaskList;

import java.util.HashMap;
import java.util.List;

import io.realm.Realm;
import retrofit2.adapter.rxjava.HttpException;
import rx.Observable;
import rx.schedulers.Schedulers;
import timber.log.Timber;

public final class TasksHelper {

    private final TasksApi mTasksApi;
    private final PrefHelper mPrefHelper;

    public TasksHelper(TasksApi tasksApi, PrefHelper prefHelper) {
        mTasksApi = tasksApi;
        mPrefHelper = prefHelper;
    }

    private Observable handleResourceNotModified(Throwable throwable) {
        // End the stream if the status code is 304 - Not Modified
        if (throwable instanceof HttpException)
            if (((HttpException) throwable).code() == 304)
                return Observable.empty();
        return Observable.error(throwable);
    }

    public Observable<List<TaskList>> getTaskLists(Realm realm) {
        return realm.where(TaskListsResponse.class)
                .findFirstAsync()
                .<TaskListsResponse>asObservable()
                .filter(taskListsResponse -> taskListsResponse.isLoaded())
                .filter(taskListsResponse -> taskListsResponse.isValid())
                .map(taskListsResponse -> taskListsResponse.items);
    }

    public Observable<TaskList> getTaskList(String taskListId, Realm realm) {
        return realm.where(TaskList.class).equalTo("id", taskListId)
                .findFirstAsync()
                .<TaskList>asObservable()
                .filter(task -> task.isLoaded())
                .filter(task -> task.isValid());
    }

    public Observable<TaskListsResponse> refreshTaskLists() {
        return mTasksApi.getTaskLists(mPrefHelper.getTaskListsResponseEtag())
                .onErrorResumeNext(this::handleResourceNotModified)
                .doOnNext(taskListsResponse -> {
                    Timber.d("handling new task list response");
                    // Save the task lists
                    Realm realm = Realm.getDefaultInstance();
                    TaskListsResponse oldTaskListResponse = realm.where(TaskListsResponse.class).findFirst();
                    if (oldTaskListResponse == null || !taskListsResponse.etag.equals(oldTaskListResponse.etag)) {
                        // Task lists have changed
                        Timber.d("Task lists have changed");
                        mPrefHelper.setTaskListsResponseEtag(taskListsResponse.etag);
                        taskListsResponse.id = mPrefHelper.getUserEmail();
                        realm.executeTransaction(realm1 -> realm1.copyToRealmOrUpdate(taskListsResponse));
                    } else Timber.d("Task lists are up-to-date");
                    realm.close();
                });
    }

    /**
     * Get the tasks associated with a given task list from the local database.
     * Also acts as a listener, pushing a new set of tasks every time they are updated.
     * Never calls onComplete.
     *
     * @param taskListId the ID of the task list
     * @param realm      an instance of Realm
     */
    public Observable<List<Task>> getTasks(String taskListId, Realm realm) {
        return realm.where(TasksResponse.class).equalTo("id", taskListId)
                .findFirstAsync()
                .<TasksResponse>asObservable()
                .filter(tasksResponse -> tasksResponse.isLoaded())
                .filter(tasksResponse -> tasksResponse.isValid())
                .map(taskResponse -> taskResponse.items);
    }

    /**
     * Get the tasks associated with a given task list from the local database.
     *
     * @param taskListId task list identifier
     * @return an Observable of a list of un-managed {@link Task}s
     */
    public Observable<List<Task>> getTasks(String taskListId) {
        return Observable.defer(() -> {
            Realm realm = Realm.getDefaultInstance();
            TasksResponse tasksResponse = realm.where(TasksResponse.class).equalTo("id", taskListId).findFirst();
            if (tasksResponse == null) {
                realm.close();
                return Observable.empty();
            } else {
                tasksResponse = realm.copyFromRealm(tasksResponse);
                realm.close();
                return Observable.just(tasksResponse.items);
            }
        });
    }

    public Observable<Task> getTask(String taskId, Realm realm) {
        return realm.where(Task.class).equalTo("id", taskId)
                .findFirstAsync()
                .<Task>asObservable()
                .filter(task -> task.isLoaded())
                .filter(task -> task.isValid());
    }

    /**
     * Sync the tasks from the specified task list that are not currently marked as synced.
     *
     * @param taskListId task list identifier
     * @return an Observable returning every task after it was successfully synced
     */
    public Observable<Task> syncTasks(String taskListId) {
        return getTasks(taskListId)
                .flatMapIterable(tasks -> tasks)
                .filter(task -> !task.isSynced())
                .flatMap(task -> updateTask(taskListId, task));
    }

    public Observable<TasksResponse> refreshTasks(String taskListId) {
        return mTasksApi.getTasks(taskListId, mPrefHelper.getTasksResponseEtag(taskListId))
                .onErrorResumeNext(this::handleResourceNotModified)
                .doOnNext(tasksResponse -> {
                    // Save the tasks if required
                    Realm realm = Realm.getDefaultInstance();
                    // Check if the task list was changed
                    TasksResponse oldTaskResponse = realm.where(TasksResponse.class).equalTo("id", taskListId).findFirst();
                    if (oldTaskResponse == null || !tasksResponse.etag.equals(oldTaskResponse.etag)) {
                        // The old task list doesn't exist or it has outdated data
                        Timber.d("Tasks have changed");
                        mPrefHelper.setTasksResponseEtag(taskListId, tasksResponse.etag);
                        tasksResponse.id = taskListId;
                        realm.executeTransaction(realm1 -> realm1.copyToRealmOrUpdate(tasksResponse));
                    } else Timber.d("Tasks are up-to-date");
                    realm.close();
                });
    }

    public Observable<Task> updateCompletionStatus(String taskListId, Task task) {
        HashMap<String, Object> taskFields = new HashMap<>();
        taskFields.put("completed", task.getCompleted());
        taskFields.put("status", task.getStatus());
        return updateTask(taskListId, task.getId(), taskFields);
    }

    public Observable<Task> updateTask(String taskListId, Task task) {
        return mTasksApi.updateTask(taskListId, task.getId(), task);
    }

    /**
     * Update the task, changing only the specified fields
     *
     * @param taskListId task list identifier
     * @param taskId     task identifier
     * @param taskFields HashMap containing the fields to be modified and their values
     */
    public Observable<Task> updateTask(String taskListId, String taskId, HashMap<String, Object> taskFields) {
        return mTasksApi.updateTask(taskListId, taskId, taskFields);
    }

    /**
     * Creates a new task in the specified task list.
     * Runs on {@link Schedulers#io()}
     *
     * @param taskListId task list identifier
     * @param task       new task
     * @return an Observable containing the full task
     */
    public Observable<Task> newTask(String taskListId, HashMap task) {
        return mTasksApi.insertTask(taskListId, task);
    }

    public Observable<Void> deleteTask(String taskListId, String taskId) {
        return mTasksApi.deleteTask(taskListId, taskId);
    }
}
