package com.teo.ttasks.ui.fragments.tasks

import com.teo.ttasks.ui.base.MvpView

/**
 * Main purpose of such interfaces — hide details of View implementation,
 * such as hundred methods of [androidx.fragment.app.Fragment].
 */
internal interface TasksView : MvpView {

    fun onTasksLoading()

    fun onTasksLoadError()

    fun onTasksEmpty()

    fun onTasksLoaded()

    fun onRefreshDone()

    fun onSyncDone(taskSyncCount: Long)
}
