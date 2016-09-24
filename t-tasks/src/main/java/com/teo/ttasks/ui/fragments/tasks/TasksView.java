package com.teo.ttasks.ui.fragments.tasks;

import android.content.Intent;
import android.support.annotation.Nullable;

import com.teo.ttasks.ui.base.MvpView;
import com.teo.ttasks.ui.items.TaskItem;

import java.util.List;

/**
 * Main purpose of such interfaces — hide details of View implementation,
 * such as hundred methods of {@link android.support.v4.app.Fragment}.
 */
interface TasksView extends MvpView {

    void onTasksLoading();

    void onActiveTasksLoaded(List<TaskItem> activeTasks);

    void onCompletedTasksLoaded(List<TaskItem> completedTasks);

    void onTasksLoadError(@Nullable Intent resolveIntent);

    void onTasksEmpty();

    void onTasksLoaded();

    void onRefreshDone();

    void onSyncDone(int taskSyncCount);
}
