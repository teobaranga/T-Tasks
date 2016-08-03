package com.teo.ttasks.ui.activities.edit_task;

import android.app.DatePickerDialog.OnDateSetListener;
import android.app.TimePickerDialog.OnTimeSetListener;

import com.teo.ttasks.data.model.Task;
import com.teo.ttasks.data.model.TaskList;
import com.teo.ttasks.ui.base.MvpView;

import java.util.List;

interface EditTaskView extends MvpView, OnDateSetListener, OnTimeSetListener {

    void onTaskLoaded(Task task);

    void onTaskListsLoaded(List<TaskList> taskLists, int selectedPosition);

    void onTaskInfoError();

    void onTaskSaved();

    void onTaskSaveError();
}
