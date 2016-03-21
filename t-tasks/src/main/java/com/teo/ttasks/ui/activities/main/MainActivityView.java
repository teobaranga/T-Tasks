package com.teo.ttasks.ui.activities.main;

import android.support.annotation.NonNull;

import com.google.android.gms.plus.model.people.Person;
import com.teo.ttasks.data.model.TaskList;
import com.teo.ttasks.ui.base.MvpView;

import java.util.List;

import io.realm.RealmResults;

public interface MainActivityView extends MvpView {
    void onUserLoaded(@NonNull Person currentPerson);

    void onCachedTaskListsLoaded(RealmResults<TaskList> taskLists);

    void onTaskListsLoaded(@NonNull List<com.google.api.services.tasks.model.TaskList> taskLists);
}
