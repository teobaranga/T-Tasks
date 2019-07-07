package com.teo.ttasks.data.remote

import com.google.firebase.database.FirebaseDatabase
import com.teo.ttasks.api.TasksApi
import com.teo.ttasks.api.entities.TaskListsResponse
import com.teo.ttasks.api.entities.TasksResponse
import com.teo.ttasks.api.entities.TasksResponseFields
import com.teo.ttasks.data.local.PrefHelper
import com.teo.ttasks.data.local.TaskFields
import com.teo.ttasks.data.local.TaskFields.Companion.taskFields
import com.teo.ttasks.data.local.TaskListFields
import com.teo.ttasks.data.model.Task
import com.teo.ttasks.data.model.Task.Companion.STATUS_COMPLETED
import com.teo.ttasks.data.model.Task.Companion.STATUS_NEEDS_ACTION
import com.teo.ttasks.data.model.TaskList
import com.teo.ttasks.jobs.TaskCreateJob
import com.teo.ttasks.jobs.TaskDeleteJob
import com.teo.ttasks.jobs.TaskUpdateJob
import com.teo.ttasks.util.FirebaseUtil.getTasksDatabase
import com.teo.ttasks.util.FirebaseUtil.saveReminder
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Single
import io.realm.*
import org.threeten.bp.ZonedDateTime
import retrofit2.HttpException
import timber.log.Timber

/**
 * Common task operations
 */
class TasksHelper(
    private val tasksApi: TasksApi,
    private val prefHelper: PrefHelper
) {
    /**
     * Create a new task list.
     *
     * @param taskListFields fields that make up the new task list
     * @return a Flowable containing the newly created task list
     */
    fun createTaskList(taskListFields: TaskListFields): Flowable<TaskList> =
        tasksApi.createTaskList(taskListFields)

    /**
     * Retrieve all the valid task lists associated with the current account.
     *
     * @param realm (optional) a Realm instance - the default Realm instance is used if not provided
     * @param async (optional)
     * @return a Flowable containing the list of task lists
     */
    fun getTaskLists(realm: Realm, async: Boolean = true): Flowable<RealmResults<TaskList>> {
        // Build the base query
        val query = queryTaskLists(realm)

        // Determine whether we're using an async query or not
        val results = if (async) query.findAllAsync() else query.findAll()

        // Convert the results to a valid Flowable
        return results
            .asFlowable()
            .filter { it.isLoaded && it.isValid }
    }

    private fun queryTaskLists(realm: Realm): RealmQuery<TaskList> =
        realm.where(TaskList::class.java)
            .equalTo(com.teo.ttasks.data.model.TaskListFields.DELETED, false)

    /**
     * Retrieve a specific task list from the local database. The requested task must be valid (not marked for deletion).
     *
     * @param taskListId task list identifier
     * @param realm      a Realm instance
     * @return a Flowable containing the requested task list
     */
    fun getTaskListAsSingle(taskListId: String, realm: Realm): Single<TaskList> =
        Single.defer {
            val taskList = getTaskList(taskListId, realm)
            if (taskList == null) {
                Single.error<TaskList>(NullPointerException("No task list found with ID $taskListId"))
            } else {
                taskList.asFlowable<TaskList>()
                    .filter { it.isValid && it.isLoaded }
                    .firstOrError()
            }
        }

    fun getTaskList(taskListId: String, realm: Realm, async: Boolean = true): TaskList? =
        realm.where(TaskList::class.java)
            .equalTo(com.teo.ttasks.data.model.TaskListFields.ID, taskListId)
            .let { if (async) it.findFirstAsync() else it.findFirst() }

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
    fun deleteTaskList(taskListId: String): Completable =
        tasksApi.deleteTaskList(taskListId)
            .doOnComplete {
                Realm.getDefaultInstance().use {
                    it.executeTransaction {
                        getTaskList(taskListId, it)?.deleteFromRealm()
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
    fun getTaskListSize(taskListId: String, realm: Realm): Long =
        queryTasks(taskListId, realm).count()

    fun refreshTaskLists(): Flowable<TaskList> =
        tasksApi.getTaskLists(prefHelper.taskListsResponseEtag)
            .onErrorResumeNext { throwable ->
                if (handleResourceNotModified(throwable)) {
                    // Short circuit if the task lists have not been modified
                    return@onErrorResumeNext Single.just(TaskListsResponse.EMPTY)
                }
                return@onErrorResumeNext Single.error(throwable)
            }
            .doOnSuccess { taskListsResponse ->
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
                            // Insert the new task lists
                            taskListsResponse.items?.forEach { taskList ->
                                getTaskList(taskList.id, it, false) ?: it.insertOrUpdate(taskList)
                            }
                        }
                    } else {
                        Timber.d("Task lists are up-to-date")
                    }
                }
            }
            .flattenAsFlowable { it.items ?: RealmList() }

    /**
     * Get the Realm-managed tasks associated with a given task list from the local database.
     * Also acts as a listener, pushing a new set of tasks every time they are updated.
     * Never calls onComplete.
     *
     * @param taskListId     the ID of the task list
     * @param realm          an instance of Realm
     * @param excludeDeleted (optional) whether to exclude locally deleted tasks - true by default
     */
    fun getTasks(taskListId: String, realm: Realm, excludeDeleted: Boolean = true, async: Boolean = true): Flowable<RealmResults<Task>> {
        return queryTasks(taskListId, realm, excludeDeleted)
            .let { return@let if (async) it.findAllAsync() else it.findAll() }
            .asFlowable()
            .filter { it.isLoaded && it.isValid }
    }

    fun getActiveTasks(taskListId: String, realm: Realm): RealmResults<Task> {
        return queryTasks(taskListId, realm)
            .equalTo(com.teo.ttasks.data.model.TaskFields.COMPLETED, null as String?)
            .sort(com.teo.ttasks.data.model.TaskFields.DUE, Sort.DESCENDING)
            .findAllAsync()
    }

    fun getCompletedTasks(taskListId: String, realm: Realm): RealmResults<Task> {
        return queryTasks(taskListId, realm)
            .notEqualTo(com.teo.ttasks.data.model.TaskFields.COMPLETED, null as String?)
            .sort(com.teo.ttasks.data.model.TaskFields.COMPLETED, Sort.DESCENDING)
            .findAllAsync()
    }

    /**
     * Get the tasks associated with a given task list from the local database.
     *
     * @param taskListId task list identifier
     * @return a Flowable of a list of un-managed [Task]s
     */
    fun getUnManagedTasks(taskListId: String): Flowable<Task> = Flowable.defer {
        lateinit var tasks: List<Task>
        Realm.getDefaultInstance().use {
            tasks = it.copyFromRealm(queryTasks(taskListId, it).findAll())
        }
        if (tasks.isEmpty()) Flowable.empty() else Flowable.fromIterable(tasks)
    }

    fun getTaskAsSingle(taskId: String, realm: Realm): Single<Task> =
        Single.defer {
            val task = getTask(taskId, realm)
            if (task == null) {
                Single.error(NullPointerException("No task found with ID $taskId"))
            } else {
                task.asFlowable<Task>()
                    .filter { it.isValid && it.isLoaded }
                    .firstOrError()
            }
        }

    /**
     * Get the first [Task] with the provided ID or null if the task is not found
     */
    fun getTask(taskId: String, realm: Realm, async: Boolean = true): Task? {
        return realm.where(Task::class.java)
            .equalTo(com.teo.ttasks.data.model.TaskFields.ID, taskId)
            .let { if (async) it.findFirstAsync() else it.findFirst() }
    }

    /**
     * Update the given task from the provided task list to the new value
     *
     * @param taskListId task list ID
     * @param task      local task containing the new values
     */
    private fun updateTask(taskListId: String, task: Task): Single<Task> {
        Timber.d("updating task %s, %s, %s", task.id, task.synced, task.deleted)
        return tasksApi.updateTask(taskListId, task.id, task).map { task }
    }

    fun deleteTask(task: Task, realm: Realm) {
        // Delete the task from the local database
        realm.executeTransaction { _ ->
            // Make sure we're deleting a managed task
            realm.copyToRealmOrUpdate(task).deleteFromRealm()
        }
    }

    /**
     * Sync all the tasks from the specified task list that are not currently marked as synced.
     * Delete the tasks that are marked as deleted.
     *
     * @param taskListId task list identifier
     * @return a Flowable with every successfully synced (updated) task
     */
    fun syncTasks(taskListId: String): Single<Long> {
        val realm = Realm.getDefaultInstance()
        return getTasks(taskListId, realm, false)
            .firstOrError()
            .flattenAsFlowable { it }
            .filter { !it.synced }
            .doOnNext {
                // Take care of the deleted tasks
                if (it.deleted) {
                    if (it.isLocalOnly) {
                        // Delete the un-synced local tasks
                        it.deleteFromRealm()
                    } else {
                        // Schedule a delete job to delete the remote and local task
                        TaskDeleteJob.schedule(it.id, taskListId)
                    }
                }
                // Handle creation
                if (it.isLocalOnly) {
                    // Schedule a job creating this task remotely
                    TaskCreateJob.schedule(it.id, taskListId)
                }
                // The remaining un-synced tasks are updates
                TaskUpdateJob.schedule(it.id, taskListId)
            }
            .doOnComplete { realm.close() }
            .count()
    }

    /**
     * Retrieve a fresh copy of the tasks from the given tasks list and update the local store
     * with the new and updated tasks
     */
    fun refreshTasks(taskListId: String): Completable =
        tasksApi.getTasks(taskListId, prefHelper.getTasksResponseEtag(taskListId))
            .onErrorResumeNext { throwable ->
                return@onErrorResumeNext if (handleResourceNotModified(throwable)) {
                    Single.just(TasksResponse.EMPTY)
                } else {
                    Single.error(throwable)
                }
            }
            .doOnSuccess { tasksResponse ->
                if (tasksResponse == TasksResponse.EMPTY) {
                    return@doOnSuccess
                }
                // Save the tasks if required
                Realm.getDefaultInstance().use { realm ->
                    // Check if the task list was changed
                    val oldTaskResponse = getTasksResponse(taskListId, realm)
                    if (oldTaskResponse == null || tasksResponse.etag != oldTaskResponse.etag) {
                        // The old task list doesn't exist or it has outdated data
                        Timber.v("Tasks have changed for %s", taskListId)
                        prefHelper.setTasksResponseEtag(taskListId, tasksResponse.etag!!)
                        tasksResponse.id = taskListId

                        realm.executeTransaction {
                            // Insert the new tasks
                            // FIXME: what happens with non-local updates?
                            it.insertOrUpdate(tasksResponse)
                            val tasksDatabase = FirebaseDatabase.getInstance().getTasksDatabase()
                            tasksResponse.items?.forEach { task ->
                                val localTask = getTask(task.id, it, false)
                                if (localTask == null) {
                                    // Insert the new task into the local storage
                                    task.taskListId = taskListId
                                    it.insertOrUpdate(task)
                                } else localTask.reminder?.let {
                                    Timber.d("saving reminder")
                                    tasksDatabase.saveReminder(localTask.id, it)
                                }
                            }
                        }
                    } else {
                        Timber.v("Tasks are up-to-date for %s", taskListId)
                    }
                }
            }
            .ignoreElement()

    /**
     * Update the task, changing only the specified fields
     *
     * @param taskListId task list identifier
     * @param taskId     task identifier
     * @param taskFields HashMap containing the fields to be modified and their values
     */
    fun updateTask(taskListId: String, taskId: String, taskFields: TaskFields): Single<Task> =
        tasksApi.updateTask(taskListId, taskId, taskFields)

    /**
     * Mark the task as completed if it's not and vice-versa.
     *
     * @param task the task whose status will change
     * @param realm a Realm instance
     * @return a Flowable containing the updated task
     */
    fun updateCompletionStatus(task: Task, realm: Realm) {
        // Update the status of the local task
        realm.executeTransaction {
            // Task is not synced at this point
            task.synced = false
            if (!task.isCompleted) {
                task.completedDate = ZonedDateTime.now()
                task.status = STATUS_COMPLETED
            } else {
                task.completedDate = null
                task.status = STATUS_NEEDS_ACTION
            }
        }

        val taskFields = taskFields {
            completed = task.completed
        }

        TaskUpdateJob.schedule(task.id, task.taskListId, taskFields)
    }

    /**
     * Mark task as completed if it's not and vice-versa.
     *
     * @param taskId task identifier
     * @param realm  a Realm instance
     * @return a Flowable containing the updated task
     */
    fun updateCompletionStatus(taskId: String, realm: Realm): Boolean {
        val task = getTask(taskId, realm)
        return if (task == null) {
            Timber.e("No task found with ID $taskId")
            false
        } else {
            updateCompletionStatus(task, realm)
            true
        }
    }

    private fun getTaskListsResponse(realm: Realm): TaskListsResponse? =
        realm.where(TaskListsResponse::class.java).findFirst()

    private fun getTasksResponse(taskListId: String, realm: Realm): TasksResponse? =
        realm.where(TasksResponse::class.java).equalTo(TasksResponseFields.ID, taskListId).findFirst()

    private fun handleResourceNotModified(throwable: Throwable): Boolean {
        // End the stream if the status code is 304 - Not Modified
        if (throwable is HttpException) {
            throwable.response()?.errorBody()?.let {
                it.close()
                Timber.v("closed error body")
            }

            if (throwable.code() == 304) {
                return true
            }
        }
        return false
    }

    /**
     * Get all the valid (not deleted) tasks from the local database.
     *
     * @param taskListId     task list identifier
     * @param realm          Realm instance
     * @param excludeDeleted (optional) whether to exclude locally deleted tasks or not - true by default
     * @return a RealmResults containing objects. If no objects match the condition, a list with zero objects is returned.
     */
    private fun queryTasks(taskListId: String, realm: Realm, excludeDeleted: Boolean = true) =
        realm.where(Task::class.java)
            .equalTo(com.teo.ttasks.data.model.TaskFields.TASK_LIST_ID, taskListId).apply {
                if (excludeDeleted) {
                    equalTo(com.teo.ttasks.data.model.TaskFields.DELETED, false)
                }
            }
}
