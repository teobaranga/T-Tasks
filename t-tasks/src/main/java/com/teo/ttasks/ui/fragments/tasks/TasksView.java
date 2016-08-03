package com.teo.ttasks.ui.fragments.tasks;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.mikepenz.fastadapter.IItem;
import com.teo.ttasks.ui.base.MvpView;

import java.util.List;

/**
 * Main purpose of such interfaces — hide details of View implementation,
 * such as hundred methods of {@link android.support.v4.app.Fragment}.
 */
interface TasksView extends MvpView {

    void onTasksLoading();

    void onTasksLoadError(@Nullable Intent resolveIntent);

    void showEmptyUi();

    void showContentUi(@NonNull List<IItem> taskItems);

    void onRefreshDone();
}
