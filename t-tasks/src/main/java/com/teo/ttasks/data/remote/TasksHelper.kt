package com.teo.ttasks.data.remote

import com.google.firebase.database.FirebaseDatabase
import com.teo.ttasks.api.TasksApi
import com.teo.ttasks.api.entities.TaskListsResponse
import com.teo.ttasks.api.entities.TasksResponse
import com.teo.ttasks.api.entities.TasksResponseFields
import com.teo.ttasks.data.local.PrefHelper
import com.teo.ttasks.data.local.TaskFields
import com.teo.ttasks.data.local.TaskListFields
import com.teo.ttasks.data.model.*
import com.teo.ttasks.data.model.Task.Companion.STATUS_COMPLETED
import com.teo.ttasks.data.model.Task.Companion.STATUS_NEEDS_ACTION
import com.teo.ttasks.util.FirebaseUtil.getTasksDatabase
import com.teo.ttasks.util.FirebaseUtil.saveReminder
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.realm.Realm
import io.realm.RealmQuery
import io.realm.RealmResults
import retrofit2.HttpException
import timber.log.Timber
import java.util.*

class TasksHelper(private val tasksApi: TasksApi, private val prefHelper: PrefHelper) {

    companion object {
        /**
         * Create a task list locally and mark it as not synced.
         * This task list needs to be copied to Realm in order to make it persistent.
         *
         * @param taskListFields task list properties
         * @return a local, un-managed task list
         */
        fun createTaskList(taskListFields: TaskListFields): TTaskList {
            // Create the task list
            val taskList = TaskList()
            taskList.title = taskListFields.title!!

            // Create the TTaskList
            val tTaskList = TTaskList(taskList)
            tTaskList.synced = false

            return tTaskList
        }
    }

    /**
     * Retrieve all the valid task lists associated with the current account.
     *
     * @param realm a Realm instance
     * @return a Flowable containing the list of task lists
     */
    fun getTaskLists(realm: Realm): Flowable<RealmResults<TTaskList>> {
        return realm.where(TTaskList::class.java)
                .equalTo(TTaskListFields.DELETED, false)
                .findAllAsync()
                .asFlowable()
    }

    /**
     * Retrieve a specific task list from the local database. The requested task must be valid (not marked for deletion).
     *
     * @param taskListId task list identifier
     * @param realm      a Realm instance
     * @return a Flowable containing the requested task list
     */
    fun getTaskListAsFlowable(taskListId: String, realm: Realm): Flowable<TTaskList> =
            getTaskList(taskListId, realm)?.asFlowable<TTaskList>() ?: Flowable.empty<TTaskList>()

    fun getTaskList(taskListId: String, realm: Realm): TTaskList? {
        return realm.where(TTaskList::class.java)
                .equalTo(TTaskListFields.ID, taskListId)
                .equalTo(TTaskListFields.DELETED, false)
                .findFirst()
    }

    /**
     * Create a new task list.
     *
     * @param taskListFields fields that make up the new task list
     * @return a Flowable containing the newly created task list
     */
    fun newTaskList(taskListFields: TaskListFields): Flowable<TaskList> = tasksApi.insertTaskList(taskListFields)

    /**
     * Update a task list by modifying specific fields (online)
     *
     * @param taskListId     task list identifier
     * @param taskListFields fields to be modified
     */
    fun updateTaskList(taskListId: String, taskListFields: TaskListFields): Flowable<TaskList> =
            tasksApi.updateTaskList(taskListId, taskListFields)

    /**
     * Delete a specific task list. The remote copy of task will be deleted first,
     * and if the operation was successful, the local copy will be removed as well.
     *
     * @param taskListId task list identifier
     */
    fun deleteTaskList(taskListId: String): Flowable<Void> {
        return tasksApi.deleteTaskList(taskListId)
                .observeOn(AndroidSchedulers.mainThread())
                .doOnComplete {
                    Realm.getDefaultInstance().use {
                        it.executeTransaction {
                            getTaskList(taskListId, it)?.deleteFromRealm()
                        }
                    }
                }
    }

    /**
     * Get the size of a specific task list.
     *
     * @param taskListId task list identifier
     * @param realm      a Realm instance
     * @return the number of tasks in the task list
     */
    fun getTaskListSize(taskListId: String, realm: Realm): Long = queryTasks(taskListId, realm).count()

    fun refreshTaskLists(): Completable {
        return tasksApi.getTaskLists(prefHelper.taskListsResponseEtag)
                .onErrorResumeNext({ throwable ->
                    if (handleResourceNotModified(throwable)) {
                        return@onErrorResumeNext Single.just(TaskListsResponse.EMPTY)
                    }
                    return@onErrorResumeNext Single.error(throwable)
                })
                .doOnSuccess({ taskListsResponse ->
                    if (taskListsResponse == TaskListsResponse.EMPTY) {
                        return@doOnSuccess
                    }
                    Timber.d("Fetching a new task list response")
                    // Save the task lists
                    Realm.getDefaultInstance().use { realm ->
                        val oldTaskListResponse = getTaskListsResponse(realm)
                        if (oldTaskListResponse == null || taskListsResponse.etag != oldTaskListResponse.etag) {
                            // Task lists have changed
                            Timber.d("Task lists have changed")
                            prefHelper.taskListsResponseEtag = taskListsResponse.etag
                            taskListsResponse.id = prefHelper.userEmail!!
                            realm.executeTransaction {
                                it.insertOrUpdate(taskListsResponse)
                                // Create a new TTaskList for each TaskList, if available
                                taskListsResponse.items?.forEach { taskList ->
                                    getTaskList(taskList.id, it) ?: it.insertOrUpdate(TTaskList(taskList))
                                }
                            }
                        } else {
                            Timber.d("Task lists are up-to-date")
                        }
                    }
                })
                .toCompletable()
    }

    /**
     * Get the Realm-managed tasks associated with a given task list from the local database.
     * Also acts as a listener, pushing a new set of tasks every time they are updated.
     * Never calls onComplete.
     *
     * @param taskListId the ID of the task list
     * @param realm      an instance of Realm
     */
    fun getTasks(taskListId: String, realm: Realm): Flowable<RealmResults<TTask>> =
            queryTasks(taskListId, realm)
                    .findAllAsync()
                    .asFlowable()
                    .filter { it.isLoaded && it.isValid }

    /**
     * Get the tasks associated with a given task list from the local database.
     *
     * @param taskListId task list identifier
     * @return a Flowable of a list of un-managed [Task]s
     */
    fun getUnManagedTasks(taskListId: String): Flowable<TTask> = Flowable.defer {
        lateinit var tasks: List<TTask>
        Realm.getDefaultInstance().use {
            tasks = it.copyFromRealm(queryTasks(taskListId, it).findAll())
        }
        if (tasks.isEmpty()) Flowable.empty() else Flowable.fromIterable(tasks)
    }

    fun getTaskAsFlowable(taskId: String, realm: Realm): Flowable<TTask> {
        return getTask(taskId, realm)?.asFlowable<TTask>()
                ?: return Flowable.error(NullPointerException("No task found with ID $taskId"))
    }

    fun getTask(taskId: String, realm: Realm): TTask? =
            realm.where(TTask::class.java).equalTo(TTaskFields.ID, taskId).findFirst()

    /**
     * Sync the tasks from the specified task list that are not currently marked as synced.
     * Delete the tasks that are marked as deleted. TODO: is this correct?
     *
     * @param taskListId task list identifier
     * @return a Flowable returning every task after it was successfully synced
     */
    fun syncTasks(taskListId: String): Flowable<TTask> =
            getUnManagedTasks(taskListId)
                    .filter {
                        // Handle unsynced tasks
                        !it.synced && !it.isLocalOnly
                    }
                    .flatMapSingle { updateTask(taskListId, it) }

    fun refreshTasks(taskListId: String): Completable {
        return tasksApi.getTasks(taskListId, prefHelper.getTasksResponseEtag(taskListId))
                .onErrorResumeNext({ throwable ->
                    if (handleResourceNotModified(throwable)) {
                        return@onErrorResumeNext Single.just(TasksResponse.EMPTY)
                    }
                    return@onErrorResumeNext Single.error(throwable)
                })
                .doOnSuccess({ tasksResponse ->
                    if (tasksResponse == TasksResponse.EMPTY) {
                        return@doOnSuccess
                    }
                    // Save the tasks if required
                    Realm.getDefaultInstance().use { realm ->
                        // Check if the task list was changed
                        val oldTaskResponse = getTasksResponse(taskListId, realm)
                        if (oldTaskResponse == null || tasksResponse.etag != oldTaskResponse.etag) {
                            // The old task list doesn't exist or it has outdated data
                            Timber.d("Tasks have changed")
                            prefHelper.setTasksResponseEtag(taskListId, tasksResponse.etag!!)
                            tasksResponse.id = taskListId

                            realm.executeTransaction {
                                it.insertOrUpdate(tasksResponse)
                                // Create a new TTask for each Task, if the task list isn't empty
                                val tasksDatabase = FirebaseDatabase.getInstance().getTasksDatabase()
                                tasksResponse.items?.forEach { task ->
                                    val tTask = getTask(task.id, it)
                                    if (tTask == null) {
                                        it.insertOrUpdate(TTask(task, taskListId))
                                    } else tTask.reminder?.let {
                                        Timber.d("saving reminder")
                                        tasksDatabase.saveReminder(tTask.id, it.time)
                                    }

                                }
                            }
                        } else {
                            Timber.d("Tasks are up-to-date")
                        }
                    }
                })
                .toCompletable()
    }

    /**
     * Update the task, changing only the specified fields
     *
     * @param taskListId task list identifier
     * @param taskId     task identifier
     * @param taskFields HashMap containing the fields to be modified and their values
     */
    fun updateTask(taskListId: String, taskId: String, taskFields: TaskFields): Flowable<Task> =
            tasksApi.updateTask(taskListId, taskId, taskFields)

    /**
     * Mark the task as completed if it's not and vice-versa.
     *
     * @param tTask the task whose status will change
     * @param realm a Realm instance
     * @return a Flowable containing the updated task
     */
    fun updateCompletionStatus(tTask: TTask, realm: Realm): Flowable<TTask> {
        // Update the status of the local task
        realm.executeTransaction {
            // Task is not synced at this point
            tTask.synced = false
            if (!tTask.isCompleted) {
                tTask.completed = Date()
                tTask.status = STATUS_COMPLETED
            } else {
                tTask.completed = null
                tTask.status = STATUS_NEEDS_ACTION
            }
        }

        val taskFields = TaskFields()
        taskFields.putCompleted(tTask.isCompleted, tTask.completed)
        return updateTask(tTask.taskListId, tTask.id, taskFields)
                .map { tTask }
                .observeOn(AndroidSchedulers.mainThread())
                .doOnComplete {
                    // Update successful, update sync status
                    realm.executeTransaction { tTask.synced = true }
                }
    }

    /**
     * Mark task as completed if it's not and vice-versa.
     *
     * @param taskId task identifier
     * @param realm  a Realm instance
     * @return a Flowable containing the updated task
     */
    fun updateCompletionStatus(taskId: String, realm: Realm): Flowable<TTask> {
        getTask(taskId, realm)?.let { task -> return updateCompletionStatus(task, realm) }
                ?: return Flowable.error<TTask>(RuntimeException("Task not found"))
    }

    private fun getTaskListsResponse(realm: Realm): TaskListsResponse? =
            realm.where(TaskListsResponse::class.java).findFirst()

    private fun getTasksResponse(taskListId: String, realm: Realm): TasksResponse? =
            realm.where(TasksResponse::class.java).equalTo(TasksResponseFields.ID, taskListId).findFirst()

    private fun handleResourceNotModified(throwable: Throwable): Boolean {
        // End the stream if the status code is 304 - Not Modified
        if (throwable is HttpException) {
            throwable.response().errorBody()?.let { it.close(); Timber.v("closed error body") }

            if (throwable.code() == 304) {
                return true
            }
        }
        return false
    }

    /**
     * Get all the valid (not deleted) tasks from the local database.
     *
     * @param taskListId task list identifier
     * @param realm      Realm instance
     * @return a RealmResults containing objects. If no objects match the condition, a list with zero objects is returned.
     */
    private fun queryTasks(taskListId: String, realm: Realm): RealmQuery<TTask> {
        return realm.where(TTask::class.java)
                .equalTo(TTaskFields.TASK_LIST_ID, taskListId)
                .equalTo(TTaskFields.DELETED, false)
    }

    private fun updateTask(taskListId: String, tTask: TTask): Single<TTask> {
        Timber.d("updating task %s, %s, %s", tTask.id, tTask.synced, tTask.deleted)
        return tasksApi.updateTask(taskListId, tTask.id, tTask.task).map { tTask }
    }
}
