package com.teo.ttasks.ui.activities.task_detail

import com.teo.ttasks.data.model.TTaskList
import com.teo.ttasks.data.model.Task
import com.teo.ttasks.ui.base.MvpView

internal interface TaskDetailView : MvpView {

    fun onTaskLoaded(task: Task)

    fun onTaskLoadError()

    fun onTaskListLoaded(taskList: TTaskList)

    fun onTaskListLoadError()

    fun onTaskUpdated(task: Task)

    fun onTaskDeleted()
}
