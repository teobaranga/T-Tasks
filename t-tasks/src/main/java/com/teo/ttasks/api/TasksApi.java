package com.teo.ttasks.api;

import com.teo.ttasks.api.entities.TaskListsResponse;
import com.teo.ttasks.api.entities.TasksResponse;
import com.teo.ttasks.data.model.Task;

import java.util.HashMap;

import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import rx.Observable;

public interface TasksApi {

    /**
     * Get the user's task lists
     *
     * @param ETag the ETag used to check if the data on the server has changed
     */
    @GET("users/@me/lists")
    Observable<TaskListsResponse> getTaskLists(@Header("If-None-Match") String ETag);

    /**
     * Get the tasks associated with a given task list
     *
     * @param taskListId the ID of the task list
     * @param ETag       the ETag used to check if the data on the server has changed
     */
    @GET("lists/{taskList}/tasks")
    Observable<TasksResponse> getTasks(@Path("taskList") String taskListId, @Header("If-None-Match") String ETag);

    /**
     * Update a task from the specified task list
     *
     * @param taskListId task list identifier
     * @param taskId     task identifier
     */
    @PATCH("lists/{taskList}/tasks/{task}/")
    Observable<Task> updateTask(@Path("taskList") String taskListId, @Path("task") String taskId, @Body HashMap taskFields);

    @PUT("lists/{taskList}/tasks/{task}/")
    Observable<Task> updateTask(@Path("taskList") String taskListId, @Path("task") String taskId, @Body Task task);

    /**
     * Creates a new task on the specified task list
     *
     * @param taskListId task list identifier
     * @param task       the new task
     */
    @POST("lists/{taskList}/tasks")
    Observable<Task> insertTask(@Path("taskList") String taskListId, @Body HashMap task);

    @DELETE("lists/{taskList}/tasks/{task}/")
    Observable<Void> deleteTask(@Path("taskList") String taskListId, @Path("task") String taskId);
}
