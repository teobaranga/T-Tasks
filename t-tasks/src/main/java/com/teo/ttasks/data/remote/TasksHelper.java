package com.teo.ttasks.data.remote;

import android.support.annotation.NonNull;

import com.google.api.services.tasks.Tasks;
import com.google.api.services.tasks.model.Task;
import com.google.api.services.tasks.model.TaskList;

import java.io.IOException;
import java.util.List;

import javax.inject.Inject;

import rx.Observable;

public class TasksHelper {

    @NonNull
    private Tasks mTasks;

    @Inject
    public TasksHelper(@NonNull Tasks tasks) {
        mTasks = tasks;
    }

    /**
     * Get all the task lists from Google
     */
    @NonNull
    public Observable<List<TaskList>> getTaskLists() {
        return Observable.defer(() -> {
            try {
                List<TaskList> taskLists = mTasks.tasklists().list().execute().getItems();
                if (taskLists == null)
                    return Observable.empty();
                else
                    return Observable.just(taskLists);
            } catch (IOException e) {
                return Observable.error(e);
            }
        });
    }

    /**
     * Get the tasks belonging to the specified task list
     */
    @NonNull
    public Observable<List<Task>> getTasks(String taskListId) {
        return Observable.defer(() -> {
            try {
                List<Task> tasks = mTasks.tasks().list(taskListId).execute().getItems();
                if (tasks == null)
                    return Observable.empty();
                else
                    return Observable.just(tasks);
            } catch (IOException e) {
                return Observable.error(e);
            }
        });
    }

}
