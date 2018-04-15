package com.teo.ttasks.widget.configure

import com.teo.ttasks.data.model.TaskList
import com.teo.ttasks.ui.base.MvpView

internal interface TasksWidgetConfigureView : MvpView {

    fun onTaskListsLoaded(taskLists: List<TaskList>)

    fun onTaskListsLoadError()
}
