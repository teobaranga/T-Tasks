package com.teo.ttasks.widget.configure;

import android.support.annotation.NonNull;

import com.teo.ttasks.data.local.PrefHelper;
import com.teo.ttasks.data.remote.TasksHelper;
import com.teo.ttasks.ui.base.Presenter;

import io.realm.Realm;

public class TasksWidgetConfigurePresenter extends Presenter<TasksWidgetConfigureView> {

    private final TasksHelper mTasksHelper;
    private final PrefHelper mPrefHelper;

    private Realm mRealm;

    public TasksWidgetConfigurePresenter(TasksHelper tasksHelper, PrefHelper prefHelper) {
        mTasksHelper = tasksHelper;
        mPrefHelper = prefHelper;
    }

    void loadTaskLists() {
        mTasksHelper.getTaskLists(mRealm)
                .subscribe(
                        taskLists -> {
                            final TasksWidgetConfigureView view = view();
                            if (view != null) view.onTaskListsLoaded(taskLists);
                        },
                        throwable -> {
                            final TasksWidgetConfigureView view = view();
                            if (view != null) view.onTaskListsLoadError();
                        }
                );
    }

    void saveWidgetTaskListId(int appWidgetId, String taskListId) {
        mPrefHelper.setWidgetTaskListId(appWidgetId, taskListId);
    }

    @Override
    public void bindView(@NonNull TasksWidgetConfigureView view) {
        super.bindView(view);
        mRealm = Realm.getDefaultInstance();
    }

    @Override
    public void unbindView(@NonNull TasksWidgetConfigureView view) {
        super.unbindView(view);
        mRealm.close();
    }
}
