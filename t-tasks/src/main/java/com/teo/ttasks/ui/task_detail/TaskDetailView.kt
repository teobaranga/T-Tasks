package com.teo.ttasks.ui.task_detail

import com.teo.ttasks.data.model.Task
import com.teo.ttasks.data.model.TaskList
import com.teo.ttasks.ui.base.MvpView

internal interface TaskDetailView : MvpView {

    fun onTaskLoaded(task: Task)

    fun onTaskLoadError()

    fun onTaskListLoaded(taskList: TaskList)

    fun onTaskListLoadError()

    fun onTaskUpdated(task: Task)

    fun onTaskDeleted()
}
