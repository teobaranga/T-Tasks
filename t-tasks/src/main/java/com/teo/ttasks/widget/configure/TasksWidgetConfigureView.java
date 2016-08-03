package com.teo.ttasks.widget.configure;

import com.teo.ttasks.data.model.TaskList;
import com.teo.ttasks.ui.base.MvpView;

import java.util.List;

interface TasksWidgetConfigureView extends MvpView {

    void onTaskListsLoaded(List<TaskList> taskLists);

    void onTaskListsLoadError();
}
