package com.teo.ttasks.util

import com.teo.ttasks.data.model.Task
import com.teo.ttasks.ui.items.TaskItem
import io.reactivex.Flowable
import io.reactivex.FlowableTransformer
import io.reactivex.SingleTransformer
import java.util.*
import kotlin.collections.ArrayList

object RxUtils {

    /**
     * 1. Creates [TaskItem]s from [Task]s<br></br>
     * 2. Groups them by completion status (not completed or no due date first followed by completed ones)<br></br>
     * 3. Sorts the first group by due date and the second group by completion date
     */
    fun getTaskItems(hideCompleted: Boolean): SingleTransformer<List<Task>, List<TaskItem>> =
        SingleTransformer { observable ->
            observable
                .map { tasks ->
                    val taskItems = ArrayList<TaskItem>()
                    val activeTasks = ArrayList<TaskItem>()
                    val completedTasks = ArrayList<TaskItem>()

                    for (task in tasks) {
                        if (task.completed == null) {
                            // Active task
                            activeTasks.add(TaskItem(task, TaskType.ACTIVE))
                        } else if (!hideCompleted) {
                            // Completed task
                            completedTasks.add(TaskItem(task, TaskType.COMPLETED))
                        }
                    }

                    // Sort active tasks by due date in descending order
                    activeTasks.sort()
                    taskItems.addAll(activeTasks)

                    if (!hideCompleted) {
                        Collections.sort(completedTasks, TaskItem.completionDateComparator)
                        if (completedTasks.size > 0) {
                            taskItems.addAll(completedTasks)
                        }
                    }

                    return@map taskItems
                }
        }

    fun getTaskItems(sortingMode: SortType): FlowableTransformer<List<Task>, Pair<TaskType, List<TaskItem>>> =
        FlowableTransformer { observable ->
            observable
                .flatMap { tasks ->
                    val activeTasks = mutableListOf<TaskItem>()
                    val completedTasks = mutableListOf<TaskItem>()

                    for (task in tasks) {
                        if (task.completed == null) {
                            // Active task
                            activeTasks.add(TaskItem(task, TaskType.ACTIVE))
                        } else {
                            // Completed task
                            completedTasks.add(TaskItem(task, TaskType.COMPLETED))
                        }
                    }

                    when (sortingMode) {
                        SortType.SORT_DATE -> {
                            // Sort active tasks by due date in ascending order
                            activeTasks.sort()
                            // Sort completed tasks by completion date in descending order
                            completedTasks.sortWith(TaskItem.completionDateComparator)
                        }
                        SortType.SORT_ALPHA -> {
                            activeTasks.sortWith(TaskItem.alphabeticalComparator)
                            completedTasks.sortWith(TaskItem.alphabeticalComparator)
                        }
                        SortType.SORT_CUSTOM -> {
                            // Do nothing
                        }
                    }

                    return@flatMap Flowable.just(
                        Pair(TaskType.ACTIVE, activeTasks.toList()),
                        Pair(TaskType.COMPLETED, completedTasks.toList()),
                        Pair(TaskType.EOT, emptyList())
                    )
                }
        }
}
