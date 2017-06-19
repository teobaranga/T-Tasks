package com.teo.ttasks.widget;

import android.content.Intent;
import android.widget.RemoteViewsService;

import com.teo.ttasks.data.remote.TasksHelper;

import javax.inject.Inject;

import dagger.android.AndroidInjection;

public class TasksWidgetService extends RemoteViewsService {

    @Inject TasksHelper tasksHelper;

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        AndroidInjection.inject(this);
        return new TasksRemoteViewsFactory(getApplicationContext(), intent, tasksHelper);
    }
}
