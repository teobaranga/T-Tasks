package com.teo.ttasks.widget.configure

import com.teo.ttasks.data.model.TTaskList
import com.teo.ttasks.ui.base.MvpView

internal interface TasksWidgetConfigureView : MvpView {

    fun onTaskListsLoaded(taskLists: List<TTaskList>)

    fun onTaskListsLoadError()
}
