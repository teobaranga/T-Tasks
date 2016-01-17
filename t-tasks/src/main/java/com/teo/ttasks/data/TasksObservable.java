package com.teo.ttasks.data;

import com.google.api.services.tasks.Tasks;
import com.google.api.services.tasks.model.Task;
import com.google.api.services.tasks.model.TaskList;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import rx.Observable;

public class TasksObservable {

    public static Observable<TaskList> getTaskLists(Tasks taskService) {
        return Observable.defer(() -> {
            List<TaskList> taskLists;
            try {
                taskLists = taskService.tasklists().list().execute().getItems();
                if (taskLists == null)
                    return Observable.from(new ArrayList<>());
            } catch (IOException e) {
                return Observable.error(e);
            }
            return Observable.from(taskLists);
        });
    }

    public static Observable<Task> getTasks(Tasks taskService, String taskListId) {
        return Observable.defer(() -> {
            List<Task> tasks;
            try {
                tasks = taskService.tasks().list(taskListId).execute().getItems();
                if (tasks == null)
                    return Observable.from(new ArrayList<>());
            } catch (IOException e) {
                return Observable.error(e);
            }
            return Observable.from(tasks);
        });
    }
}
