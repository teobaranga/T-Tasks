package com.teo.ttasks.ui.fragments.task_lists;

import com.mikepenz.fastadapter.IItem;
import com.teo.ttasks.ui.base.MvpView;

import java.util.List;

public interface TaskListsView extends MvpView {

    void onTaskListsLoading();

    void onTaskListsEmpty();

    void onTaskListsError();

    void onTaskListsLoaded(List<IItem> taskListItems);
}
