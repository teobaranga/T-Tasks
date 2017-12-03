package com.teo.ttasks.ui.activities.main

import com.teo.ttasks.data.model.TTaskList
import com.teo.ttasks.ui.base.MvpView

interface MainView : MvpView {

    /** Called when the user has a profile picture  */
    fun onUserPicture(pictureUrl: String)

    /** Called when the user has a cover picture  */
    fun onUserCover(coverUrl: String)

    fun onTaskListsLoaded(taskLists: List<TTaskList>, currentTaskListIndex: Int)

    fun onTaskListsLoadError()
}
