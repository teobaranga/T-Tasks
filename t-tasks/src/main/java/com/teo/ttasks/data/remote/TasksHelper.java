package com.teo.ttasks.data.remote;

import android.support.annotation.NonNull;

import com.google.api.services.tasks.Tasks;
import com.google.api.services.tasks.model.Task;
import com.google.api.services.tasks.model.TaskList;

import java.io.IOException;
import java.util.ArrayList;
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
    public Observable<TaskList> getTaskLists() {
        return Observable.defer(() -> {
            List<TaskList> taskLists;
            try {
                taskLists = mTasks.tasklists().list().execute().getItems();
                if (taskLists == null)
                    return Observable.from(new ArrayList<>());
            } catch (IOException e) {
                return Observable.error(e);
            }
            return Observable.from(taskLists);
        });
    }

    /**
     * Get the tasks belonging to the specified task list
     */
    @NonNull
    public Observable<Task> getTasks(String taskListId) {
        return Observable.defer(() -> {
            List<Task> tasks;
            try {
                tasks = mTasks.tasks().list(taskListId).execute().getItems();
                if (tasks == null)
                    return Observable.from(new ArrayList<>());
            } catch (IOException e) {
                return Observable.error(e);
            }
            return Observable.from(tasks);
        });
    }

}
