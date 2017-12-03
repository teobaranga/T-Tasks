package com.teo.ttasks.ui.fragments.task_lists

import com.teo.ttasks.ui.base.MvpView
import com.teo.ttasks.ui.items.TaskListItem

internal interface TaskListsView : MvpView {

    fun onTaskListsLoading()

    fun onTaskListsEmpty()

    fun onTaskListsError()

    fun onTaskListsLoaded(taskListItems: List<TaskListItem>)

    fun onTaskListUpdateError()
}
