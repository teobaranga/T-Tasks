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

import io.reactivex.Flowable;
import io.reactivex.FlowableTransformer;
import io.reactivex.flowables.GroupedFlowable;

import static java.lang.annotation.RetentionPolicy.SOURCE;

public class RxUtils {

    private RxUtils() { }

    /**
     * 1. Creates {@link TaskItem}s from {@link Task}s<br>
     * 2. Groups them by completion status (not completed or no due date first followed by completed ones)<br>
     * 3. Sorts the first group by due date and the second group by completion date
     */
    public static FlowableTransformer<List<TTask>, List<IItem>> getTaskItems(boolean hideCompleted) {
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

    public static FlowableTransformer<List<TTask>, GroupedFlowable<Boolean, List<TaskItem>>> getTaskItems(@SortingMode int sortingMode) {
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

                    switch (sortingMode) {
                        case SORT_DATE:
                            // Sort active tasks by due date in ascending order
                            Collections.sort(activeTasks);
                            // Sort completed tasks by completion date in descending order
                            Collections.sort(completedTasks, TaskItem.completionDateComparator);
                            break;
                        case SORT_ALPHA:
                            Collections.sort(activeTasks, TaskItem.alphabeticalComparator);
                            Collections.sort(completedTasks, TaskItem.alphabeticalComparator);
                            break;
                        case SORT_MY_ORDER:
                            // Do nothing
                            break;
                    }

                    return Flowable.just(activeTasks, completedTasks)
                            // Group by pushing the active tasks as true and completed tasks as false
                            .groupBy(task -> !task.isEmpty() && task.get(0).getCompleted() == null);
                });
    }

    @Retention(SOURCE)
    @IntDef({SORT_DATE, SORT_ALPHA, SORT_MY_ORDER})
    public @interface SortingMode {}

    public static final int SORT_DATE = 0;
    public static final int SORT_ALPHA = 1;
    public static final int SORT_MY_ORDER = 2;
}
