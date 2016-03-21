package com.teo.ttasks.ui.fragments.tasks;

import android.support.annotation.NonNull;

import com.teo.ttasks.injection.AnyThread;
import com.teo.ttasks.ui.base.MvpView;
import com.teo.ttasks.ui.items.TaskItem;

import java.util.List;

/**
 * Main purpose of such interfaces — hide details of View implementation,
 * such as hundred methods of {@link android.support.v4.app.Fragment}.
 */
public interface TasksView extends MvpView {

    // Presenter does not know about Main Thread. It's a detail of View implementation!
    @AnyThread
    void showLoadingUi();

    @AnyThread
    void showErrorUi();

    @AnyThread
    void showEmptyUi();

    @AnyThread
    void showContentUi(@NonNull List<TaskItem> taskItems);

}
