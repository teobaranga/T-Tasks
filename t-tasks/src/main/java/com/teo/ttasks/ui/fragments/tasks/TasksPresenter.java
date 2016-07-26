package com.teo.ttasks.ui.fragments.tasks;

import android.support.annotation.NonNull;

import com.teo.ttasks.data.remote.TasksHelper;
import com.teo.ttasks.ui.base.Presenter;
import com.teo.ttasks.util.RxUtil;

import io.realm.Realm;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import timber.log.Timber;

public class TasksPresenter extends Presenter<TasksView> {

    private final TasksHelper mTasksHelper;

    private Realm mRealm;

    public TasksPresenter(TasksHelper tasksHelper) {
        mTasksHelper = tasksHelper;
    }

    void getTasks(String taskListId) {
        final Subscription subscription = mTasksHelper.getTasks(taskListId, mRealm)
                .compose(RxUtil.getTaskItems())
                .subscribe(
                        tasks -> {
                            Timber.d("loaded %d tasks", tasks.size());
                            final TasksView view = view();
                            if (view != null) {
                                if (tasks.isEmpty()) view.showEmptyUi();
                                else view.showContentUi(tasks);
                            }
                        },
                        throwable -> {
                            // TODO: 2016-07-12 error
                        });
        unsubscribeOnUnbindView(subscription);
    }

    void refreshTasks(String taskListId) {
        final Subscription subscription = mTasksHelper.refreshTasks(taskListId)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        tasksResponse -> {
                        },
                        throwable -> Timber.e(throwable.toString()),
                        () -> {
                            final TasksView view = view();
                            if (view != null) view.onRefreshDone();
                        }
                );
        unsubscribeOnUnbindView(subscription);
    }

    @Override
    public void bindView(@NonNull TasksView view) {
        super.bindView(view);
        mRealm = Realm.getDefaultInstance();
    }

    @Override
    public void unbindView(@NonNull TasksView view) {
        super.unbindView(view);
        mRealm.close();
    }
}
