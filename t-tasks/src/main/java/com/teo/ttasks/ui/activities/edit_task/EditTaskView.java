package com.teo.ttasks.ui.activities.edit_task;

import android.app.DatePickerDialog.OnDateSetListener;
import android.app.TimePickerDialog.OnTimeSetListener;
import android.view.View;

import com.teo.ttasks.data.model.TTask;
import com.teo.ttasks.data.model.TTaskList;
import com.teo.ttasks.ui.base.MvpView;

import java.util.List;

public interface EditTaskView extends MvpView, OnDateSetListener, OnTimeSetListener {

    void onTaskLoaded(TTask task);

    void onTaskListsLoaded(List<TTaskList> taskLists, int selectedPosition);

    void onTaskLoadError();

    void onTaskSaved(TTask task);

    void onTaskSaveError();

    boolean onDueDateLongClicked(View v);

    boolean onDueTimeLongClicked(View v);

    void onTitleChanged(CharSequence title, int start, int before, int count);

    void onNotesChanged(CharSequence notes, int start, int before, int count);
}
