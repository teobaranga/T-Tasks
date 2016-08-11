package com.teo.ttasks.data.remote;

import com.teo.ttasks.api.TasksApi;
import com.teo.ttasks.api.entities.TaskListsResponse;
import com.teo.ttasks.api.entities.TasksResponse;
import com.teo.ttasks.data.local.PrefHelper;
import com.teo.ttasks.data.model.TTask;
import com.teo.ttasks.data.model.Task;
import com.teo.ttasks.data.model.TaskList;

import java.util.Date;
import java.util.HashMap;
import java.util.List;

import io.realm.Realm;
import io.realm.RealmResults;
import retrofit2.adapter.rxjava.HttpException;
import rx.Observable;
import rx.schedulers.Schedulers;
import timber.log.Timber;

import static com.teo.ttasks.data.model.Task.STATUS_COMPLETED;
import static com.teo.ttasks.data.model.Task.STATUS_NEEDS_ACTION;

public final class TasksHelper {

    private final TasksApi tasksApi;
    private final PrefHelper prefHelper;

    public TasksHelper(TasksApi tasksApi, PrefHelper prefHelper) {
        this.tasksApi = tasksApi;
        this.prefHelper = prefHelper;
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
        return tasksApi.getTaskLists(prefHelper.getTaskListsResponseEtag())
                .onErrorResumeNext(this::handleResourceNotModified)
                .doOnNext(taskListsResponse -> {
                    Timber.d("handling new task list response");
                    // Save the task lists
                    Realm realm = Realm.getDefaultInstance();
                    TaskListsResponse oldTaskListResponse = realm.where(TaskListsResponse.class).findFirst();
                    if (oldTaskListResponse == null || !taskListsResponse.etag.equals(oldTaskListResponse.etag)) {
                        // Task lists have changed
                        Timber.d("Task lists have changed");
                        prefHelper.setTaskListsResponseEtag(taskListsResponse.etag);
                        taskListsResponse.id = prefHelper.getUserEmail();
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
    public Observable<RealmResults<TTask>> getTasks(String taskListId, Realm realm) {
        return realm.where(TTask.class).equalTo("taskListId", taskListId)
                .findAllAsync()
                .asObservable()
                .filter(RealmResults::isLoaded)
                .filter(RealmResults::isValid);
    }

    /**
     * Get the tasks associated with a given task list from the local database.
     *
     * @param taskListId task list identifier
     * @return an Observable of a list of un-managed {@link Task}s
     */
    public Observable<List<TTask>> getTasks(String taskListId) {
        return Observable.defer(() -> {
            Realm realm = Realm.getDefaultInstance();
            List<TTask> tasks = realm.where(TTask.class).equalTo("taskListId", taskListId).findAll();
            if (tasks.isEmpty()) {
                realm.close();
                return Observable.empty();
            } else {
                tasks = realm.copyFromRealm(tasks);
                realm.close();
                return Observable.just(tasks);
            }
        });
    }

    public Observable<TTask> getTask(String taskId, Realm realm) {
        return realm.where(TTask.class).equalTo("id", taskId)
                .findFirstAsync()
                .<TTask>asObservable()
                .filter(task -> task.isLoaded())
                .filter(task -> task.isValid());
    }

    /**
     * Sync the tasks from the specified task list that are not currently marked as synced.
     *
     * @param taskListId task list identifier
     * @return an Observable returning every task after it was successfully synced
     */
    public Observable<TTask> syncTasks(String taskListId) {
        return getTasks(taskListId)
                .flatMapIterable(tasks -> tasks)
                .filter(task -> !task.isSynced())
                .flatMap(task -> updateTask(taskListId, task));
    }

    public Observable<TasksResponse> refreshTasks(String taskListId) {
        return tasksApi.getTasks(taskListId, prefHelper.getTasksResponseEtag(taskListId))
                .onErrorResumeNext(this::handleResourceNotModified)
                .doOnNext(tasksResponse -> {
                    // Save the tasks if required
                    Realm realm = Realm.getDefaultInstance();
                    // Check if the task list was changed
                    TasksResponse oldTaskResponse = realm.where(TasksResponse.class).equalTo("id", taskListId).findFirst();
                    if (oldTaskResponse == null || !tasksResponse.etag.equals(oldTaskResponse.etag)) {
                        // The old task list doesn't exist or it has outdated data
                        Timber.d("Tasks have changed");
                        prefHelper.setTasksResponseEtag(taskListId, tasksResponse.etag);
                        tasksResponse.id = taskListId;

                        realm.executeTransaction(realm1 -> {
                            realm1.copyToRealmOrUpdate(tasksResponse);
                            // Create a new TTask for each Task
                            for (Task task : tasksResponse.items) {
                                TTask tTask = realm1.where(TTask.class).equalTo("id", task.getId()).findFirst();
                                if (tTask == null)
                                    realm1.copyToRealmOrUpdate(new TTask(task, taskListId));
                            }
                        });
                    } else Timber.d("Tasks are up-to-date");
                    realm.close();
                });
    }

    /**
     * Mark the task as completed if it's not and vice-versa.
     *
     * @param taskListId task list identifier
     * @param tTask      the task whose status will change
     * @param realm      a Realm instance
     * @return an Observable containing the updated task
     */
    public Observable<TTask> updateCompletionStatus(String taskListId, TTask tTask, Realm realm) {
        // Update the status of the local task
        realm.executeTransaction(realm1 -> {
            // Task is not synced at this point
            tTask.setSynced(false);
            boolean completed = tTask.getCompleted() != null;
            if (!completed) {
                tTask.setCompleted(new Date());
                tTask.setStatus(STATUS_COMPLETED);
                Timber.d("task was completed");
            } else {
                tTask.setCompleted(null);
                tTask.setStatus(STATUS_NEEDS_ACTION);
                Timber.d("task needs action");
            }
        });

        HashMap<String, Object> taskFields = new HashMap<>();
        taskFields.put("completed", tTask.getCompleted());
        taskFields.put("status", tTask.getStatus());
        return updateTask(taskListId, tTask.getId(), taskFields)
                .map(task -> tTask);
    }

    public Observable<TTask> updateCompletionStatus(String taskListId, String taskId, Realm realm) {
        TTask tTask = realm.where(TTask.class).equalTo("id", taskId).findFirst();
        if (tTask != null) {
            return updateCompletionStatus(taskListId, tTask, realm);
        }
        return Observable.error(new RuntimeException("Task not found"));
    }

    public Observable<TTask> updateTask(String taskListId, TTask tTask) {
        return tasksApi.updateTask(taskListId, tTask.getId(), tTask.task)
                .map(task -> tTask);
    }

    /**
     * Update the task, changing only the specified fields
     *
     * @param taskListId task list identifier
     * @param taskId     task identifier
     * @param taskFields HashMap containing the fields to be modified and their values
     */
    public Observable<Task> updateTask(String taskListId, String taskId, HashMap<String, Object> taskFields) {
        return tasksApi.updateTask(taskListId, taskId, taskFields);
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
        return tasksApi.insertTask(taskListId, task);
    }

    public Observable<Void> deleteTask(String taskListId, String taskId) {
        return tasksApi.deleteTask(taskListId, taskId);
    }
}
