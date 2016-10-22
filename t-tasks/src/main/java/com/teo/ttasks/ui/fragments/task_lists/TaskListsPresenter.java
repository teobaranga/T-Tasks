package com.teo.ttasks.ui.fragments.task_lists;

import android.support.annotation.NonNull;

import com.teo.ttasks.data.model.TTaskList;
import com.teo.ttasks.data.local.TaskListFields;
import com.teo.ttasks.data.remote.TasksHelper;
import com.teo.ttasks.ui.base.Presenter;
import com.teo.ttasks.ui.items.TaskListItem;

import java.util.ArrayList;
import java.util.List;

import io.realm.Realm;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import timber.log.Timber;

public class TaskListsPresenter extends Presenter<TaskListsView> {

    private final TasksHelper tasksHelper;

    private TaskListFields taskListFields;

    private Realm realm;

    public TaskListsPresenter(TasksHelper tasksHelper) {
        this.tasksHelper = tasksHelper;
        taskListFields = new TaskListFields();
    }

    /**
     * Load all the user's task lists.
     */
    void getTaskLists() {
        {
            final TaskListsView view = view();
            if (view != null) view.onTaskListsLoading();
        }
        final Subscription subscription = tasksHelper.getTaskLists(realm)
                .map(taskLists -> {
                    List<TaskListItem> taskListItems = new ArrayList<>(taskLists.size());

                    for (int i = 0, taskListsSize = taskLists.size(); i < taskListsSize; i++) {
                        TTaskList taskList = taskLists.get(i);
                        taskListItems.add(new TaskListItem(taskList, tasksHelper.getTaskListSize(taskList.getId(), realm)));
                    }

                    return taskListItems;
                })
                .subscribe(taskListItems -> {
                    final TaskListsView view = view();
                    if (view != null) {
                        if (taskListItems == null) view.onTaskListsError();
                        else if (taskListItems.isEmpty()) view.onTaskListsEmpty();
                        else view.onTaskListsLoaded(taskListItems);
                    }
                });
        unsubscribeOnUnbindView(subscription);
    }

    /**
     * Set the task list title.
     *
     * @param title the new task list title
     */
    void setTaskListTitle(String title) {
        taskListFields.putTitle(title);
    }

    void createTaskList() {
        // Nothing entered
        if (taskListFields.isEmpty())
            return;

        // Create the task list offline
        TTaskList tTaskList = TasksHelper.createTaskList(taskListFields);
        Timber.d("New task list with id %s", tTaskList.getId());
        realm.executeTransaction(realm -> realm.insertOrUpdate(tTaskList));

        // Create the task remotely
        tasksHelper.newTaskList(taskListFields)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        taskList -> {
                            // Update the local task with the full information and delete the old task
                            TTaskList managedTaskList = tasksHelper.getTaskList(tTaskList.getId(), realm);
                            realm.executeTransaction(realm -> {
                                managedTaskList.getTaskList().deleteFromRealm();
                                managedTaskList.switchTaskList(realm.copyToRealm(taskList));
                                managedTaskList.setSynced(true);
                            });
                            Timber.d("Task list id updated to %s", managedTaskList.getId());
                        },
                        throwable -> {
                            Timber.e(throwable.toString());
                        }
                );
    }

    /**
     * Update the task list with the specified ID. Currently, only a title change is supported.
     *
     * @param taskListId task list identifier
     * @param isOnline   flag indicating an active network connection
     */
    void updateTaskList(String taskListId, boolean isOnline) {
        // Nothing changed
        if (taskListFields.isEmpty())
            return;

        // Update locally
        final TTaskList managedTaskList = tasksHelper.getTaskList(taskListId, realm);
        realm.executeTransaction(realm -> {
            managedTaskList.update(taskListFields);
            managedTaskList.setSynced(false);
        });

        // Update remotely
        if (isOnline) {
            tasksHelper.updateTaskList(taskListId, taskListFields)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            taskList -> {
                                realm.executeTransaction(realm -> {
                                    realm.insertOrUpdate(taskList);
                                    managedTaskList.setSynced(true);
                                });
                            },
                            throwable -> {
                                Timber.e(throwable.toString());
                                final TaskListsView view = view();
                                if (view != null) view.onTaskListUpdateError();
                            }
                    );
        }
    }

    /**
     * Delete the task list with the specified ID.
     *
     * @param taskListId task list identifier
     */
    void deleteTaskList(String taskListId) {
        // Get the task list
        final TTaskList managedTaskList = tasksHelper.getTaskList(taskListId, realm);

        // Mark it as deleted so it doesn't show up in the list
        realm.executeTransaction(realm -> managedTaskList.setDeleted(true));

        // Delete the task list
        tasksHelper.deleteTaskList(taskListId)
                .subscribe(
                        aVoid -> { /* Do nothing */ },
                        throwable -> {
                            Timber.e(throwable.toString());
                        }
                );
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
