package com.teo.ttasks.ui.fragments.tasks

import android.content.Intent

import com.teo.ttasks.ui.base.MvpView
import com.teo.ttasks.ui.items.TaskItem

/**
 * Main purpose of such interfaces — hide details of View implementation,
 * such as hundred methods of [android.support.v4.app.Fragment].
 */
internal interface TasksView : MvpView {

    fun onTasksLoading()

    fun onActiveTasksLoaded(activeTasks: List<TaskItem>)

    fun onCompletedTasksLoaded(completedTasks: List<TaskItem>)

    fun onTasksLoadError(resolveIntent: Intent?)

    fun onTasksEmpty()

    fun onTasksLoaded()

    fun onRefreshDone()

    fun onSyncDone(taskSyncCount: Int)
}
