package com.teo.ttasks.ui.activities.main;

import android.support.annotation.NonNull;

import com.teo.ttasks.data.model.TTaskList;
import com.teo.ttasks.ui.base.MvpView;

import java.util.List;

interface MainView extends MvpView {

    /** Called when the user has a profile picture */
    void onUserPicture(@NonNull String pictureUrl);

    /** Called when the user has a cover picture */
    void onUserCover(@NonNull String coverUrl);

    void onTaskListsLoaded(List<TTaskList> taskLists, int currentTaskListIndex);

    void onTaskListsLoadError();
}
