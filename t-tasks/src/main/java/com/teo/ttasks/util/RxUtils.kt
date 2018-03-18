package com.teo.ttasks.util

import com.teo.ttasks.data.model.TTask
import com.teo.ttasks.data.model.Task
import com.teo.ttasks.ui.items.TaskItem
import io.reactivex.Flowable
import io.reactivex.FlowableTransformer
import io.reactivex.flowables.GroupedFlowable
import java.util.*
import kotlin.collections.ArrayList

object RxUtils {

    /**
     * 1. Creates [TaskItem]s from [Task]s<br></br>
     * 2. Groups them by completion status (not completed or no due date first followed by completed ones)<br></br>
     * 3. Sorts the first group by due date and the second group by completion date
     */
    fun getTaskItems(hideCompleted: Boolean): FlowableTransformer<List<TTask>, List<TaskItem>> {
        return FlowableTransformer { observable ->
            observable
                    .map({ tasks ->
                        val taskItems = ArrayList<TaskItem>()
                        val activeTasks = ArrayList<TaskItem>()
                        val completedTasks = ArrayList<TaskItem>()

                        for (task in tasks) {
                            if (task.completed == null) {
                                // Active task
                                activeTasks.add(TaskItem(task))
                            } else if (!hideCompleted) {
                                // Completed task
                                completedTasks.add(TaskItem(task))
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

                        taskItems
                    })
        }
    }

    fun getTaskItems(sortingMode: SortType): FlowableTransformer<List<TTask>, GroupedFlowable<Boolean, List<TaskItem>>> {
        return FlowableTransformer { observable ->
            observable
                    .flatMap { tTasks ->
                        val activeTasks = mutableListOf<TaskItem>()
                        val completedTasks = mutableListOf<TaskItem>()

                        for (task in tTasks) {
                            if (task.completed == null) {
                                // Active task
                                activeTasks.add(TaskItem(task))
                            } else {
                                // Completed task
                                completedTasks.add(TaskItem(task))
                            }
                        }

                        when (sortingMode) {
                            SortType.SORT_DATE -> {
                                // Sort active tasks by due date in ascending order
                                activeTasks.sort()
                                // Sort completed tasks by completion date in descending order
                                Collections.sort(completedTasks, TaskItem.completionDateComparator)
                            }
                            SortType.SORT_ALPHA -> {
                                Collections.sort(activeTasks, TaskItem.alphabeticalComparator)
                                Collections.sort(completedTasks, TaskItem.alphabeticalComparator)
                            }
                            SortType.SORT_CUSTOM -> {
                            }
                        }// Do nothing

                        Flowable.just(activeTasks.toList(), completedTasks.toList())
                                .groupBy { task -> !task.isEmpty() && task[0].completed == null }
                    }
        }
    }
}
