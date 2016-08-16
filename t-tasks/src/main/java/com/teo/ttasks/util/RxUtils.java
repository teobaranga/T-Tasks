package com.teo.ttasks.util;

import android.support.annotation.NonNull;

import com.mikepenz.fastadapter.IItem;
import com.teo.ttasks.data.model.TTask;
import com.teo.ttasks.data.model.Task;
import com.teo.ttasks.ui.items.CategoryItem;
import com.teo.ttasks.ui.items.TaskItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.realm.RealmResults;
import rx.Observable;
import rx.Observable.Transformer;

public class RxUtils {

    private RxUtils() { }

    /**
     * 1. Creates {@link TaskItem}s from {@link Task}s<br>
     * 2. Groups them by completion status (not completed or no due date first followed by completed ones)<br>
     * 3. Sorts the first group by due date and the second group by completion date
     */
    @NonNull
    public static Transformer<RealmResults<TTask>, List<IItem>> getTaskItems() {
        return observable -> observable
                .flatMap(tasks -> {
                    List<TaskItem> activeTasks = new ArrayList<>();
                    List<TaskItem> completedTasks = new ArrayList<>();
                    List<IItem> taskItems = new ArrayList<>();
                    for (TTask task : tasks) {
                        if (task.getCompleted() == null) {
                            // Active task
                            activeTasks.add(new TaskItem(task));
                        } else {
                            // Completed task
                            completedTasks.add(new TaskItem(task));
                        }
                    }
                    Collections.sort(activeTasks);
                    taskItems.addAll(activeTasks);

                    Collections.sort(completedTasks, TaskItem.completionDateComparator);
                    if (completedTasks.size() > 0) {
                        taskItems.add(new CategoryItem().withName("Completed"));
                        taskItems.addAll(completedTasks);
                    }

                    return Observable.just(taskItems);
                });
    }
}