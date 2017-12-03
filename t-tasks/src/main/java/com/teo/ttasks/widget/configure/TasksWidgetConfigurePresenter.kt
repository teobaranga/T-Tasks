package com.teo.ttasks.widget.configure

import com.teo.ttasks.data.local.PrefHelper
import com.teo.ttasks.data.remote.TasksHelper
import com.teo.ttasks.ui.base.Presenter

import io.realm.Realm

internal class TasksWidgetConfigurePresenter(private val mTasksHelper: TasksHelper, private val mPrefHelper: PrefHelper) : Presenter<TasksWidgetConfigureView>() {

    private lateinit var realm: Realm

    internal fun loadTaskLists() {
        mTasksHelper.getTaskLists(realm)
                .subscribe({
                    taskLists -> view()?.onTaskListsLoaded(taskLists)
                }, {
                    view()?.onTaskListsLoadError()
                })
    }

    internal fun saveWidgetTaskListId(appWidgetId: Int, taskListId: String) {
        mPrefHelper.setWidgetTaskListId(appWidgetId, taskListId)
    }

    override fun bindView(view: TasksWidgetConfigureView) {
        super.bindView(view)
        realm = Realm.getDefaultInstance()
    }

    override fun unbindView(view: TasksWidgetConfigureView) {
        super.unbindView(view)
        realm.close()
    }
}
