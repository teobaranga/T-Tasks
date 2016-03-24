package com.teo.ttasks.util;

import com.teo.ttasks.data.model.Task;
import com.teo.ttasks.ui.items.TaskItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.realm.RealmResults;
import rx.Observable;
import rx.Observable.Transformer;

public class RxUtil {

    /**
     * 1. Creates {@link TaskItem}s from {@link Task}s<br>
     * 2. Groups them by completion status (not completed or no due date first followed by completed ones)<br>
     * 3. Sorts the first group by due date and the second group by completion date
     */
    public static Transformer<RealmResults<Task>, List<TaskItem>> getTaskItems() {
        return observable -> observable
                .flatMap(tasks -> {
                    List<TaskItem> taskItems = new ArrayList<>();
                    for (Task task : tasks)
                        taskItems.add(new TaskItem(task));
                    return Observable.from(taskItems);
                })
                .groupBy(taskItem -> taskItem.getCompleted() != null && taskItem.getDueDate() != null)
                .flatMap(groupedTaskObservable -> {
                    if (groupedTaskObservable.getKey())
                        return groupedTaskObservable
                                .toList()
                                .map(taskItems -> {
                                    Collections.sort(taskItems, TaskItem.completionDateComparator);
                                    return taskItems;
                                });
                    else
                        return groupedTaskObservable
                                .toList()
                                .map(taskItems -> {
                                    Collections.sort(taskItems);
                                    return taskItems;
                                });
                })
                .flatMapIterable(taskItems -> taskItems)
                .toList();
    }

}
