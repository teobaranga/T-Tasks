package com.teo.ttasks.ui.activities.main

import com.teo.ttasks.data.model.TaskList
import com.teo.ttasks.ui.base.MvpView

interface MainView : MvpView {

    /** Called when the user has a cover picture  */
    fun onUserCover(coverUrl: String)

    fun onTaskListsLoaded(taskLists: List<TaskList>, currentTaskListIndex: Int)

    fun onTaskListsLoadError()

    fun onSignedOut()
}
