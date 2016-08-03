package com.teo.ttasks.widget;

import android.content.Intent;
import android.widget.RemoteViewsService;

public class TasksWidgetService extends RemoteViewsService {

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new TasksRemoteViewsFactory(getApplicationContext(), intent);
    }
}
