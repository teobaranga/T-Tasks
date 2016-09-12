package com.teo.ttasks.widget.configure;

import com.teo.ttasks.data.model.TTaskList;
import com.teo.ttasks.ui.base.MvpView;

import java.util.List;

interface TasksWidgetConfigureView extends MvpView {

    void onTaskListsLoaded(List<TTaskList> taskLists);

    void onTaskListsLoadError();
}
