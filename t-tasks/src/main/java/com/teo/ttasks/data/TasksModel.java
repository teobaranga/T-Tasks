package com.teo.ttasks.data;

import android.support.annotation.NonNull;

import com.google.api.services.tasks.Tasks;
import com.google.api.services.tasks.model.Task;
import com.google.api.services.tasks.model.TaskList;

import javax.inject.Inject;

import rx.Observable;

public class TasksModel {

    @NonNull
    private Tasks mTasks;

    @Inject
    public TasksModel(@NonNull Tasks tasks) {
        mTasks = tasks;
    }

    /**
     * Get all the task lists from Google
     */
    @NonNull
    public Observable<TaskList> getTaskLists() {
        return TasksObservable.getTaskLists(mTasks);
    }

    /**
     * Get the tasks belonging to the specified task list
     */
    @NonNull
    public Observable<Task> getTasks(String taskListId) {
        return TasksObservable.getTasks(mTasks, taskListId);
    }

}
