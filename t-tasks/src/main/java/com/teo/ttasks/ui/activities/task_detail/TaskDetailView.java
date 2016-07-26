package com.teo.ttasks.ui.activities.task_detail;

import com.teo.ttasks.data.model.Task;
import com.teo.ttasks.data.model.TaskList;
import com.teo.ttasks.ui.base.MvpView;

interface TaskDetailView extends MvpView {

    void onTaskLoaded(Task task);

    void onTaskLoadError();

    void onTaskListLoaded(TaskList taskList);

    void onTaskListLoadError();

    void onTaskUpdated();

    void onTaskUpdateError();

    void onTaskDeleted();
}
