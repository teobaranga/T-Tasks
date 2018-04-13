package com.teo.ttasks.api

import com.teo.ttasks.api.entities.TaskListsResponse
import com.teo.ttasks.api.entities.TasksResponse
import com.teo.ttasks.data.local.TaskFields
import com.teo.ttasks.data.local.TaskListFields
import com.teo.ttasks.data.model.Task
import com.teo.ttasks.data.model.TaskList
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Single
import retrofit2.http.*

interface TasksApi {

    /* Task Lists */

    /** Create a new task list */
    @POST("users/@me/lists")
    fun createTaskList(@Body taskListFields: TaskListFields): Flowable<TaskList>

    /**
     * Get the user's task lists
     *
     * @param ETag the ETag used to check if the data on the server has changed
     */
    @GET("users/@me/lists")
    fun getTaskLists(@Header("If-None-Match") ETag: String): Single<TaskListsResponse>

    /** Update a task list */
    @PATCH("users/@me/lists/{taskList}")
    fun updateTaskList(@Path("taskList") taskListId: String, @Body taskListFields: TaskListFields): Flowable<TaskList>

    /** Delete a task list */
    @DELETE("users/@me/lists/{taskList}")
    fun deleteTaskList(@Path("taskList") taskListId: String): Flowable<Void>

    /* Tasks */

    /**
     * Creates a new task in the specified task list
     *
     * @param taskListId task list identifier
     * @param taskFields the fields that the new task will contain
     */
    @POST("lists/{taskList}/tasks")
    fun createTask(@Path("taskList") taskListId: String, @Body taskFields: TaskFields): Single<Task>

    /**
     * Creates a new task in the specified task list
     *
     * @param taskListId task list identifier
     * @param task the new task
     */
    @POST("lists/{taskList}/tasks")
    fun createTask(@Path("taskList") taskListId: String, @Body task: Task): Single<Task>

    /**
     * Get the tasks associated with a given task list
     *
     * @param taskListId the ID of the task list
     * @param ETag       the ETag used to check if the data on the server has changed
     */
    @GET("lists/{taskList}/tasks")
    fun getTasks(@Path("taskList") taskListId: String, @Header("If-None-Match") ETag: String): Single<TasksResponse>

    /**
     * Update a task from the specified task list, updating only the specified fields
     *
     * @param taskListId task list identifier
     * @param taskId     task identifier
     * @param taskFields map of fields to their new values
     */
    @PATCH("lists/{taskList}/tasks/{task}/")
    fun updateTask(@Path("taskList") taskListId: String, @Path("task") taskId: String, @Body taskFields: TaskFields): Single<Task>

    /**
     * Update the given task from the specified task list, replacing all fields with the provided ones
     *
     * @param taskListId task list identifier
     * @param taskId     task identifier
     * @param task       task containing the new values
     */
    @PUT("lists/{taskList}/tasks/{task}/")
    fun updateTask(@Path("taskList") taskListId: String, @Path("task") taskId: String, @Body task: Task): Single<Task>

    /**
     * Delete a task from the Google servers and then remove the local copy as well.
     *
     * @param taskListId task list identifier
     * @param taskId     task identifier
     */
    @DELETE("lists/{taskList}/tasks/{task}")
    fun deleteTask(@Path("taskList") taskListId: String, @Path("task") taskId: String): Completable
}
