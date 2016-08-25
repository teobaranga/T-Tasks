package com.teo.ttasks.ui.fragments.task_lists;

import android.support.annotation.NonNull;

import com.teo.ttasks.data.remote.TasksHelper;
import com.teo.ttasks.ui.base.Presenter;
import com.teo.ttasks.util.RxUtils;

import io.realm.Realm;
import rx.Subscription;
import timber.log.Timber;

public class TaskListsPresenter extends Presenter<TaskListsView> {

    private final TasksHelper tasksHelper;

    private Realm realm;

    public TaskListsPresenter(TasksHelper tasksHelper) {
        this.tasksHelper = tasksHelper;
    }

    void getTaskLists() {
        {
            final TaskListsView view = view();
            if (view != null) view.onTaskListsLoading();
        }
        final Subscription subscription = tasksHelper.getTaskLists(realm)
                .compose(RxUtils.getTaskListItems())
                .subscribe(
                        taskListItems -> {
                            final TaskListsView view = view();
                            if (view != null) {
                                if (taskListItems.isEmpty()) view.onTaskListsEmpty();
                                else view.onTaskListsLoaded(taskListItems);
                            }
                        },
                        throwable -> {
                            Timber.e(throwable.toString());
                            final TaskListsView view = view();
                            if (view != null) view.onTaskListsError();
                        });
        unsubscribeOnUnbindView(subscription);
    }

    @Override
    public void bindView(@NonNull TaskListsView view) {
        super.bindView(view);
        realm = Realm.getDefaultInstance();
    }

    @Override
    public void unbindView(@NonNull TaskListsView view) {
        super.unbindView(view);
        realm.close();
    }
}
