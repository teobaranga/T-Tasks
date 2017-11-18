package com.teo.ttasks.data.remote;

import android.annotation.SuppressLint;
import android.support.annotation.Nullable;

import com.google.firebase.database.DatabaseReference;
import com.teo.ttasks.api.TasksApi;
import com.teo.ttasks.api.entities.TaskListsResponse;
import com.teo.ttasks.api.entities.TasksResponse;
import com.teo.ttasks.api.entities.TasksResponseFields;
import com.teo.ttasks.data.local.PrefHelper;
import com.teo.ttasks.data.local.TaskFields;
import com.teo.ttasks.data.local.TaskListFields;
import com.teo.ttasks.data.model.TTask;
import com.teo.ttasks.data.model.TTaskFields;
import com.teo.ttasks.data.model.TTaskList;
import com.teo.ttasks.data.model.TTaskListFields;
import com.teo.ttasks.data.model.Task;
import com.teo.ttasks.data.model.TaskList;
import com.teo.ttasks.util.FirebaseUtil;

import java.util.Date;
import java.util.List;

import io.reactivex.Flowable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.realm.Realm;
import io.realm.RealmQuery;
import io.realm.RealmResults;
import okhttp3.ResponseBody;
import retrofit2.HttpException;
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

    private TaskListsResponse getTaskListsResponse(Realm realm) {
        return realm.where(TaskListsResponse.class).findFirst();
    }

    private TasksResponse getTasksResponse(String taskListId, Realm realm) {
        return realm.where(TasksResponse.class).equalTo(TasksResponseFields.ID, taskListId).findFirst();
    }

    private Flowable handleResourceNotModified(Throwable throwable) {
        // End the stream if the status code is 304 - Not Modified
        if (throwable instanceof HttpException) {
            final HttpException httpException = (HttpException) throwable;
            final ResponseBody errorBody = httpException.response().errorBody();
            if (errorBody != null) {
                Timber.d("closing error body");
                errorBody.close();
            }
            if (httpException.code() == 304)
                return Flowable.empty();
        }
        return Flowable.error(throwable);
    }

    /**
     * Retrieve all the valid task lists associated with the current account.
     *
     * @param realm a Realm instance
     * @return a Flowable containing the list of task lists
     */
    public Flowable<RealmResults<TTaskList>> getTaskLists(Realm realm) {
        return realm.where(TTaskList.class)
                .equalTo(TTaskListFields.DELETED, false)
                .findAll()
                .asFlowable();
    }

    /**
     * Retrieve a specific task list from the local database. The requested task must be valid (not marked for deletion).
     *
     * @param taskListId task list identifier
     * @param realm      a Realm instance
     * @return a Flowable containing the requested task list
     */
    public Flowable<TTaskList> getTaskListAsFlowable(String taskListId, Realm realm) {
        final TTaskList taskList = getTaskList(taskListId, realm);
        if (taskList == null)
            return Flowable.empty();
        return taskList.asFlowable();
    }

    public TTaskList getTaskList(String taskListId, Realm realm) {
        return realm.where(TTaskList.class)
                .equalTo(TTaskListFields.ID, taskListId)
                .equalTo(TTaskListFields.DELETED, false)
                .findFirst();
    }

    /**
     * Create a new task list.
     *
     * @param taskListFields fields that make up the new task list
     * @return an Flowable containing the newly created task list
     */
    public Flowable<TaskList> newTaskList(TaskListFields taskListFields) {
        return tasksApi.insertTaskList(taskListFields);
    }

    /**
     * Update a task list by modifying specific fields (online)
     *
     * @param taskListId     task list identifier
     * @param taskListFields fields to be modified
     */
    public Flowable<TaskList> updateTaskList(String taskListId, TaskListFields taskListFields) {
        return tasksApi.updateTaskList(taskListId, taskListFields);
    }

    /**
     * Delete a specific task list. The remote copy of task will be deleted first,
     * and if the operation was successful, the local copy will be removed as well.
     *
     * @param taskListId task list identifier
     */
    public Flowable<Void> deleteTaskList(String taskListId) {
        return tasksApi.deleteTaskList(taskListId)
                .observeOn(AndroidSchedulers.mainThread())
                .doOnComplete(() -> {
                    Realm realm = Realm.getDefaultInstance();
                    realm.executeTransaction(realm1 -> {
                        final TTaskList tTaskList = getTaskList(taskListId, realm1);
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

    public Flowable<TaskListsResponse> refreshTaskLists() {
        return tasksApi.getTaskLists(prefHelper.getTaskListsResponseEtag())
                .onErrorResumeNext(this::handleResourceNotModified)
                .doOnNext(taskListsResponse -> {
                    Timber.d("handling new task list response");
                    // Save the task lists
                    Realm realm = Realm.getDefaultInstance();
                    TaskListsResponse oldTaskListResponse = getTaskListsResponse(realm);
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
                                    TTaskList tTaskList = getTaskList(taskList.getId(), realm1);
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
                .equalTo(TTaskFields.TASK_LIST_ID, taskListId)
                .equalTo(TTaskFields.DELETED, false);
    }

    /**
     * Get the tasks associated with a given task list from the local database.
     * Also acts as a listener, pushing a new set of tasks every time they are updated.
     * Never calls onComplete.
     *
     * @param taskListId the ID of the task list
     * @param realm      an instance of Realm
     */
    public Flowable<RealmResults<TTask>> getTasks(String taskListId, Realm realm) {
        return getValidTasks(taskListId, realm).findAll().asFlowable();
    }

    /**
     * Get the tasks associated with a given task list from the local database.
     *
     * @param taskListId task list identifier
     * @return a Flowable of a list of un-managed {@link Task}s
     */
    public Flowable<List<TTask>> getTasks(String taskListId) {
        return Flowable.defer(() -> {
            Realm realm = Realm.getDefaultInstance();
            List<TTask> tasks = getValidTasks(taskListId, realm).findAll();
            if (tasks.isEmpty()) {
                realm.close();
                return Flowable.empty();
            } else {
                tasks = realm.copyFromRealm(tasks);
                realm.close();
                return Flowable.just(tasks);
            }
        });
    }

    public Flowable<TTask> getTaskAsFlowable(String taskId, Realm realm) {
        final TTask tTask = getTask(taskId, realm);
        if (tTask == null) {
            return Flowable.error(new NullPointerException("No task found with ID " + taskId));
        }
        return tTask.asFlowable();
    }

    @Nullable
    public TTask getTask(String taskId, Realm realm) {
        return realm.where(TTask.class).equalTo(TTaskFields.ID, taskId).findFirst();
    }

    /**
     * Sync the tasks from the specified task list that are not currently marked as synced.
     * Delete the tasks that are marked as deleted.
     *
     * @param taskListId task list identifier
     * @return a Flowable returning every task after it was successfully synced
     */
    @SuppressLint("NewApi")
    public Flowable<TTask> syncTasks(String taskListId) {
        return getTasks(taskListId)
                .flatMapIterable(tasks -> tasks)
                .filter(task -> !task.isSynced())
                .flatMap(tTask -> {
                    // These tasks are not managed by Realm
                    // Handle unsynced tasks
                    if (!tTask.isSynced()) {
                        if (!tTask.isLocalOnly()) {
                            return updateTask(taskListId, tTask);
                        }
                    }
                    return Flowable.empty();
                });
    }

    public Flowable<TasksResponse> refreshTasks(String taskListId) {
        return tasksApi.getTasks(taskListId, prefHelper.getTasksResponseEtag(taskListId))
                .onErrorResumeNext(this::handleResourceNotModified)
                .doOnNext(tasksResponse -> {
                    // Save the tasks if required
                    Realm realm = Realm.getDefaultInstance();
                    // Check if the task list was changed
                    TasksResponse oldTaskResponse = getTasksResponse(taskListId, realm);
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
                                    TTask tTask = getTask(task.getId(), realm1);
                                    if (tTask == null) {
                                        realm1.insertOrUpdate(new TTask(task, taskListId));
                                    } else if (tTask.getReminder() != null) {
                                        Timber.d("saving reminder");
                                        FirebaseUtil.saveReminder(reference, tTask.getId(), tTask.getReminder().getTime());
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

    private Flowable<TTask> updateTask(String taskListId, TTask tTask) {
        Timber.d("updating task %s, %s, %s", tTask.getId(), tTask.isSynced(), tTask.isDeleted());
        return tasksApi.updateTask(taskListId, tTask.getId(), tTask.task).map(task -> tTask);
    }

    /**
     * Update the task, changing only the specified fields
     *
     * @param taskListId task list identifier
     * @param taskId     task identifier
     * @param taskFields HashMap containing the fields to be modified and their values
     */
    public Flowable<Task> updateTask(String taskListId, String taskId, TaskFields taskFields) {
        return tasksApi.updateTask(taskListId, taskId, taskFields);
    }

    /**
     * Mark the task as completed if it's not and vice-versa.
     *
     * @param tTask the task whose status will change
     * @param realm a Realm instance
     * @return a Flowable containing the updated task
     */
    public Flowable<TTask> updateCompletionStatus(TTask tTask, Realm realm) {
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
                .doOnComplete(() -> {
                    // Update successful, update sync status
                    realm.executeTransaction(realm1 -> tTask.setSynced(true));
                });
    }

    /**
     * Mark task as completed if it's not and vice-versa.
     *
     * @param taskId task identifier
     * @param realm  a Realm instance
     * @return a Flowable containing the updated task
     */
    public Flowable<TTask> updateCompletionStatus(String taskId, Realm realm) {
        TTask tTask = getTask(taskId, realm);
        if (tTask != null)
            return updateCompletionStatus(tTask, realm);
        return Flowable.error(new RuntimeException("Task not found"));
    }
}
