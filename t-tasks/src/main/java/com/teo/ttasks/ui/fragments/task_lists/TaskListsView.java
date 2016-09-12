package com.teo.ttasks.ui.fragments.task_lists;

import com.teo.ttasks.ui.base.MvpView;
import com.teo.ttasks.ui.items.TaskListItem;

import java.util.List;

interface TaskListsView extends MvpView {

    void onTaskListsLoading();

    void onTaskListsEmpty();

    void onTaskListsError();

    void onTaskListsLoaded(List<TaskListItem> taskListItems);

    void onTaskListUpdateError();
}
