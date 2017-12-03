package com.teo.ttasks.ui.activities.edit_task

import android.app.DatePickerDialog.OnDateSetListener
import android.app.TimePickerDialog.OnTimeSetListener
import android.view.View

import com.teo.ttasks.data.model.TTask
import com.teo.ttasks.data.model.TTaskList
import com.teo.ttasks.ui.base.MvpView

internal interface EditTaskView : MvpView, OnDateSetListener, OnTimeSetListener {

    fun onTaskLoaded(task: TTask)

    fun onTaskListsLoaded(taskLists: List<TTaskList>, selectedPosition: Int)

    fun onTaskLoadError()

    fun onTaskSaved()

    fun onTaskSaveError()

    fun onDueTimeLongClicked(v: View): Boolean

    fun onTitleChanged(title: CharSequence, start: Int, before: Int, count: Int)

    fun onNotesChanged(notes: CharSequence, start: Int, before: Int, count: Int)
}
