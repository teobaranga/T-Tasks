package com.teo.ttasks.ui.fragments.tasks;

import android.support.annotation.NonNull;

import com.teo.ttasks.injection.AnyThread;
import com.teo.ttasks.data.model.Task;

import io.realm.RealmResults;

/**
 * Main purpose of such interfaces — hide details of View implementation,
 * such as hundred methods of {@link android.support.v4.app.Fragment}.
 */
public interface TasksView {

    // Presenter does not know about Main Thread. It's a detail of View implementation!
    @AnyThread
    void showLoadingUi();

    @AnyThread
    void showErrorUi();

    @AnyThread
    void showEmptyUi();

    @AnyThread
    void showContentUi(@NonNull RealmResults<Task> items);

}
