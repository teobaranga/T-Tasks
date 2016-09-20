package com.teo.ttasks.util;

import android.support.annotation.IntDef;

import com.mikepenz.fastadapter.IItem;
import com.teo.ttasks.R;
import com.teo.ttasks.data.model.TTask;
import com.teo.ttasks.data.model.Task;
import com.teo.ttasks.ui.items.CategoryItem;
import com.teo.ttasks.ui.items.TaskItem;

import java.lang.annotation.Retention;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import rx.Observable;
import rx.Observable.Transformer;
import rx.observables.GroupedObservable;

import static java.lang.annotation.RetentionPolicy.SOURCE;

public class RxUtils {

    private RxUtils() { }

    /**
     * 1. Creates {@link TaskItem}s from {@link Task}s<br>
     * 2. Groups them by completion status (not completed or no due date first followed by completed ones)<br>
     * 3. Sorts the first group by due date and the second group by completion date
     */
    public static Transformer<List<TTask>, List<IItem>> getTaskItems(boolean hideCompleted) {
        return observable -> observable
                .map(tasks -> {
                    List<IItem> taskItems = new ArrayList<>();
                    List<TaskItem> activeTasks = new ArrayList<>();
                    List<TaskItem> completedTasks = new ArrayList<>();

                    for (TTask task : tasks) {
                        if (task.getCompleted() == null) {
                            // Active task
                            activeTasks.add(new TaskItem(task));
                        } else if (!hideCompleted) {
                            // Completed task
                            completedTasks.add(new TaskItem(task));
                        }
                    }

                    // Sort active tasks by due date in descending order
                    Collections.sort(activeTasks);
                    taskItems.addAll(activeTasks);

                    if (!hideCompleted) {
                        Collections.sort(completedTasks, TaskItem.completionDateComparator);
                        if (completedTasks.size() > 0) {
                            taskItems.add(new CategoryItem().withTitle(R.string.completed));
                            taskItems.addAll(completedTasks);
                        }
                    }

                    return taskItems;
                });
    }

    public static Transformer<List<TTask>, GroupedObservable<Boolean, List<TaskItem>>> getTaskItems() {
        return observable -> observable
                .flatMap(tTasks -> {
                    List<TaskItem> activeTasks = new ArrayList<>();
                    List<TaskItem> completedTasks = new ArrayList<>();

                    for (TTask task : tTasks) {
                        if (task.getCompleted() == null) {
                            // Active task
                            activeTasks.add(new TaskItem(task));
                        } else {
                            // Completed task
                            completedTasks.add(new TaskItem(task));
                        }
                    }

                    // Sort active tasks by due date in descending order
                    Collections.sort(activeTasks);

                    Collections.sort(completedTasks, TaskItem.completionDateComparator);

                    return Observable.just(
                            GroupedObservable.from(true, Observable.just(activeTasks)),
                            GroupedObservable.from(false, Observable.just(completedTasks)));
                });
    }

    @Retention(SOURCE)
    @IntDef({1, 2, 3})
    public @interface SortingMode {}
}
