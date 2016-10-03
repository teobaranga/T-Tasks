package com.teo.ttasks.data.remote;

import android.annotation.SuppressLint;

import com.google.firebase.database.DatabaseReference;
import com.teo.ttasks.api.TasksApi;
import com.teo.ttasks.api.entities.TaskListsResponse;
import com.teo.ttasks.api.entities.TasksResponse;
import com.teo.ttasks.data.local.PrefHelper;
import com.teo.ttasks.data.model.TTask;
import com.teo.ttasks.data.model.TTaskList;
import com.teo.ttasks.data.model.Task;
import com.teo.ttasks.data.model.TaskFields;
import com.teo.ttasks.data.model.TaskList;
import com.teo.ttasks.data.model.TaskListFields;
import com.teo.ttasks.util.FirebaseUtil;

import java.util.Date;
import java.util.List;

import io.realm.Realm;
import io.realm.RealmQuery;
import io.realm.RealmResults;
import retrofit2.adapter.rxjava.HttpException;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
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

    /**
     * Create a task list locally and mark it as not synced.
     * This task list needs to be copied to Realm in order to make it persistent.
     *
     * @param taskListFields task list properties
     * @return a local, un-managed task list
     */
    public static TTaskList createTaskList(TaskListFields taskListFields) {
        // Create the task list
        final TaskList taskList = new TaskList();
        taskList.setTitle(taskListFields.getTitle());

        // Create the TTaskList
        final TTaskList tTaskList = new TTaskList(taskList);
        tTaskList.setSynced(false);

        return tTaskList;
    }

    private Observable handleResourceNotModified(Throwable throwable) {
        // End the stream if the status code is 304 - Not Modified
        if (throwable instanceof HttpException)
            if (((HttpException) throwable).code() == 304)
                return Observable.empty();
        return Observable.error(throwable);
    }

    /**
     * Retrieve all the valid task lists associated with the current account.
     *
     * @param realm a Realm instance
     * @return an Observable containing the list of task lists
     */
    public Observable<RealmResults<TTaskList>> getTaskLists(Realm realm) {
        return realm.where(TTaskList.class)
                .equalTo("deleted", false)
                .findAll()
                .asObservable();
    }

    /**
     * Retrieve a specific task list from the local database. The requested task must be valid (not marked for deletion).
     *
     * @param taskListId task list identifier
     * @param realm      a Realm instance
     * @return an Observable containing the requested task list
     */
    public Observable<TTaskList> getTaskList(String taskListId, Realm realm) {
        return realm.where(TTaskList.class)
                .equalTo("id", taskListId)
                .equalTo("deleted", false)
                .findFirst()
                .asObservable();
    }

    /**
     * Create a new task list.
     *
     * @param taskListFields fields that make up the new task list
     * @return an Observable containing the newly created task list
     */
    public Observable<TaskList> newTaskList(TaskListFields taskListFields) {
        return tasksApi.insertTaskList(taskListFields);
    }

    /**
     * Update a task list by modifying specific fields (online)
     *
     * @param taskListId     task list identifier
     * @param taskListFields fields to be modified
     */
    public Observable<TaskList> updateTaskList(String taskListId, TaskListFields taskListFields) {
        return tasksApi.updateTaskList(taskListId, taskListFields);
    }

    /**
     * Delete a specific task list. The remote copy of task will be deleted first,
     * and if the operation was successful, the local copy will be removed as well.
     *
     * @param taskListId task list identifier
     */
    public Observable<Void> deleteTaskList(String taskListId) {
        return tasksApi.deleteTaskList(taskListId)
                .observeOn(AndroidSchedulers.mainThread())
                .doOnCompleted(() -> {
                    Realm realm = Realm.getDefaultInstance();
                    realm.executeTransaction(realm1 -> {
                        final TTaskList tTaskList = realm1.where(TTaskList.class).equalTo("id", taskListId).findFirst();
                        if (tTaskList != null) {
                            // Should always be the case
                            tTaskList.deleteFromRealm();
                        }
                    });
                    realm.close();
                });
    }

    /**
     * Get the size of a specific task list.
     *
     * @param taskListId task list identifier
     * @param realm      a Realm instance
     * @return the number of tasks in the task list
     */
    public long getTaskListSize(String taskListId, Realm realm) {
        return getValidTasks(taskListId, realm).count();
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
                        realm.executeTransaction(realm1 -> {
                            realm1.insertOrUpdate(taskListsResponse);
                            // Create a new TTaskList for each TaskList, if available
                            if (taskListsResponse.items != null) {
                                for (TaskList taskList : taskListsResponse.items) {
                                    TTaskList tTaskList = realm1.where(TTaskList.class).equalTo("id", taskList.getId()).findFirst();
                                    if (tTaskList == null)
                                        realm1.insertOrUpdate(new TTaskList(taskList));
                                }
                            }
                        });
                    } else Timber.d("Task lists are up-to-date");
                    realm.close();
                });
    }

    /**
     * Get all the valid (not deleted) tasks from the local database.
     *
     * @param taskListId task list identifier
     * @param realm      Realm instance
     * @return a RealmResults containing objects. If no objects match the condition, a list with zero objects is returned.
     */
    private RealmQuery<TTask> getValidTasks(String taskListId, Realm realm) {
        return realm.where(TTask.class)
                .equalTo("taskListId", taskListId)
                .equalTo("deleted", false);
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
        return getValidTasks(taskListId, realm).findAll().asObservable();
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
            List<TTask> tasks = getValidTasks(taskListId, realm).findAll();
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
        final TTask tTask = realm.where(TTask.class).equalTo("id", taskId).findFirst();
        if (tTask == null)
            return Observable.empty();
        return tTask.asObservable();
    }

    /**
     * Sync the tasks from the specified task list that are not currently marked as synced.
     * Delete the tasks that are marked as deleted.
     *
     * @param taskListId task list identifier
     * @return an Observable returning every task after it was successfully synced
     */
    @SuppressLint("NewApi")
    public Observable<TTask> syncTasks(String taskListId) {
        return getTasks(taskListId)
                .flatMapIterable(tasks -> tasks)
                .filter(task -> !task.isSynced() || task.isDeleted())
                .flatMap(tTask -> {
                    // These tasks are not managed by Realm
                    // Handle deleted tasks first
                    if (tTask.isDeleted())
                        return deleteTask(taskListId, tTask.getId())
                                .flatMap(aVoid -> Observable.empty());
                    // Handle unsynced tasks
                    if (!tTask.isSynced()) {
                        if (tTask.isNew()) {
                            return newTask(taskListId, TaskFields.fromTask(tTask))
                                    .map(task -> {
                                        // Create a copy of the old TTask
                                        final TTask savedTask = new TTask(tTask, task);
                                        savedTask.setSynced(true);
                                        try (Realm realm = Realm.getDefaultInstance()) {
                                            realm.executeTransaction(realm1 -> {
                                                // Save the new TTask with the correct ID
                                                realm1.insertOrUpdate(savedTask);
                                                // Delete the old task
                                                tTask.getTask().deleteFromRealm();
                                                tTask.deleteFromRealm();
                                            });
                                        }

                                        prefHelper.deleteLastTaskId();

                                        // Save the reminder online
                                        if (savedTask.getReminder() != null) {
                                            final DatabaseReference tasksDatabase = FirebaseUtil.getTasksDatabase();
                                            tasksDatabase.child(savedTask.getId()).child("reminder").setValue(savedTask.getReminder().getTime());
                                        }

                                        return savedTask;
                                    });
                        } else {
                            return updateTask(taskListId, tTask);
                        }
                    }
                    return Observable.empty();
                });
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

                        final DatabaseReference reference = FirebaseUtil.getTasksDatabase();
                        realm.executeTransaction(realm1 -> {
                            realm1.insertOrUpdate(tasksResponse);
                            // Create a new TTask for each Task, if the task list isn't empty
                            if (tasksResponse.items != null) {
                                for (Task task : tasksResponse.items) {
                                    TTask tTask = realm1.where(TTask.class).equalTo("id", task.getId()).findFirst();
                                    if (tTask == null) {
                                        realm1.insertOrUpdate(new TTask(task, taskListId));
                                    } else if (tTask.getReminder() != null) {
                                        Timber.d("saving reminder");
                                        reference.child(tTask.getId()).child("reminder").setValue(tTask.getReminder().getTime());
                                    }
                                }
                            }
                        });
                    } else {
                        Timber.d("Tasks are up-to-date");
                    }
                    realm.close();
                });
    }

    /**
     * Creates a new task in the specified task list.
     * Runs on {@link Schedulers#io()}
     *
     * @param taskListId task list identifier
     * @param taskFields new task
     * @return an Observable containing the full task
     */
    public Observable<Task> newTask(String taskListId, TaskFields taskFields) {
        return tasksApi.insertTask(taskListId, taskFields);
    }

    private Observable<TTask> updateTask(String taskListId, TTask tTask) {
        Timber.d("updating task %s, %s, %s", tTask.getId(), tTask.isSynced(), tTask.isDeleted());
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
    public Observable<Task> updateTask(String taskListId, String taskId, TaskFields taskFields) {
        return tasksApi.updateTask(taskListId, taskId, taskFields);
    }

    /**
     * Mark the task as completed if it's not and vice-versa.
     *
     * @param tTask the task whose status will change
     * @param realm a Realm instance
     * @return an Observable containing the updated task
     */
    public Observable<TTask> updateCompletionStatus(TTask tTask, Realm realm) {
        // Update the status of the local task
        realm.executeTransaction(realm1 -> {
            // Task is not synced at this point
            tTask.setSynced(false);
            boolean completed = tTask.getCompleted() != null;
            if (!completed) {
                tTask.setCompleted(new Date());
                tTask.setStatus(STATUS_COMPLETED);
            } else {
                tTask.setCompleted(null);
                tTask.setStatus(STATUS_NEEDS_ACTION);
            }
        });

        TaskFields taskFields = new TaskFields();
        taskFields.putCompleted(tTask.isCompleted(), tTask.getCompleted());
        return updateTask(tTask.getTaskListId(), tTask.getId(), taskFields)
                .map(task -> tTask)
                .observeOn(AndroidSchedulers.mainThread())
                .doOnCompleted(() -> {
                    // Update successful, update sync status
                    realm.executeTransaction(realm1 -> tTask.setSynced(true));
                });
    }

    /**
     * Mark task as completed if it's not and vice-versa.
     *
     * @param taskId task identifier
     * @param realm  a Realm instance
     * @return an Observable containing the updated task
     */
    public Observable<TTask> updateCompletionStatus(String taskId, Realm realm) {
        TTask tTask = realm.where(TTask.class).equalTo("id", taskId).findFirst();
        if (tTask != null)
            return updateCompletionStatus(tTask, realm);
        return Observable.error(new RuntimeException("Task not found"));
    }

    /**
     * Delete a task from the Google servers and then remove the local copy as well.
     *
     * @param taskListId task list identifier
     * @param taskId     task identifier
     */
    public Observable<Void> deleteTask(String taskListId, String taskId) {
        return tasksApi.deleteTask(taskListId, taskId)
                .observeOn(AndroidSchedulers.mainThread())
                .doOnCompleted(() -> {
                    Realm realm = Realm.getDefaultInstance();
                    realm.executeTransaction(realm1 -> {
                        final TTask tTask = realm1.where(TTask.class).equalTo("id", taskId).findFirst();
                        if (tTask != null) {
                            // Should always be the case
                            tTask.deleteFromRealm();
                        }
                    });
                    realm.close();
                });
    }
}
