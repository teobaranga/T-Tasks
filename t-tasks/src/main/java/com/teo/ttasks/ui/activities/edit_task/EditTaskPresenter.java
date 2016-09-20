package com.teo.ttasks.ui.activities.edit_task;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.Pair;

import com.teo.ttasks.data.local.PrefHelper;
import com.teo.ttasks.data.local.WidgetHelper;
import com.teo.ttasks.data.model.TTask;
import com.teo.ttasks.data.model.TTaskList;
import com.teo.ttasks.data.model.Task;
import com.teo.ttasks.data.model.TaskFields;
import com.teo.ttasks.data.remote.TasksHelper;
import com.teo.ttasks.ui.base.Presenter;
import com.teo.ttasks.util.NotificationHelper;

import java.util.Calendar;
import java.util.Date;

import io.realm.Realm;
import rx.android.schedulers.AndroidSchedulers;
import timber.log.Timber;

// TODO: 2016-08-09 save the reminder on new task and task update
public class EditTaskPresenter extends Presenter<EditTaskView> {

    private final TasksHelper tasksHelper;
    private final PrefHelper prefHelper;
    private final WidgetHelper widgetHelper;
    private final NotificationHelper notificationHelper;

    @Nullable private Date dueDate;
    @Nullable private Date reminder;

    /**
     * Object containing the task fields that have been modified.
     */
    private TaskFields editTaskFields;

    private Realm realm;

    public EditTaskPresenter(TasksHelper tasksHelper, PrefHelper prefHelper, WidgetHelper widgetHelper,
                             NotificationHelper notificationHelper) {
        this.tasksHelper = tasksHelper;
        this.prefHelper = prefHelper;
        this.widgetHelper = widgetHelper;
        this.notificationHelper = notificationHelper;
        editTaskFields = new TaskFields();
    }

    /**
     * Load the task and display its information into the view.
     *
     * @param taskId task list identifier
     */
    void loadTaskInfo(String taskId) {
        tasksHelper.getTask(taskId, realm)
                .subscribe(
                        tTask -> {
                            if (tTask == null) {
                                // Task not found, should never happen
                                final EditTaskView view = view();
                                if (view != null) view.onTaskLoadError();
                                return;
                            }
                            dueDate = tTask.getDue();
                            reminder = tTask.getReminder();
                            final EditTaskView view = view();
                            if (view != null) view.onTaskLoaded(tTask);
                        }
                );

    }

    /**
     * Load the task lists and find the index of the provided task list in it.
     * This is used to automatically select the task list to which the current task belongs.
     *
     * @param currentTaskListId task list identifier
     */
    void loadTaskLists(String currentTaskListId) {
        tasksHelper.getTaskLists(realm)
                .map(taskLists -> {
                    // Find the index of the current task list
                    for (int i = 0; i < taskLists.size(); i++) {
                        TTaskList taskList = taskLists.get(i);
                        if (taskList.getId().equals(currentTaskListId))
                            return new Pair<>(taskLists, i);
                    }
                    // Index not found, select the first task list
                    return new Pair<>(taskLists, 0);
                })
                .subscribe(
                        taskListsIndexPair -> {
                            final EditTaskView view = view();
                            if (view != null) view.onTaskListsLoaded(taskListsIndexPair.first, taskListsIndexPair.second);
                        },
                        throwable -> {
                            Timber.e(throwable.toString());
                            final EditTaskView view = view();
                            if (view != null) view.onTaskLoadError();
                        }
                );
    }

    /**
     * Set the due date. If one isn't present, assign the new one. Otherwise, modify the old one.
     *
     * @param date the due date
     */
    void setDueDate(@Nullable Date date) {
        if (dueDate == null) {
            dueDate = date;
        } else {
            // Change only the date and not the time
            Calendar oldCal = Calendar.getInstance();
            oldCal.setTime(dueDate);

            Calendar newCal = Calendar.getInstance();
            newCal.setTime(date);

            oldCal.set(newCal.get(Calendar.YEAR), newCal.get(Calendar.MONTH), newCal.get(Calendar.DAY_OF_MONTH), 0, 0, 0);
            dueDate = oldCal.getTime();
        }
        editTaskFields.putDueDate(dueDate);
    }

    // TODO: 2016-08-19 implement this using Firebase
    void setDueTime(Date date) {
        if (dueDate == null) {
            dueDate = date;
        } else {
            Timber.d("old date %s", dueDate.toString());
            Calendar oldCal = Calendar.getInstance();
            oldCal.setTime(dueDate);

            Calendar newCal = Calendar.getInstance();
            newCal.setTime(date);

            oldCal.set(Calendar.HOUR_OF_DAY, newCal.get(Calendar.HOUR_OF_DAY));
            oldCal.set(Calendar.MINUTE, newCal.get(Calendar.MINUTE));
            dueDate = oldCal.getTime();
            Timber.d("new date %s", dueDate.toString());
        }
    }

    /**
     * Set the reminder time. This requires that the due date is already set.
     *
     * @param date the reminder time
     */
    void setReminderTime(Date date) {
        if (dueDate == null)
            return;

        Calendar oldCal = Calendar.getInstance();
        oldCal.setTime(dueDate);

        Calendar newCal = Calendar.getInstance();
        newCal.setTime(date);

        oldCal.set(Calendar.HOUR_OF_DAY, newCal.get(Calendar.HOUR_OF_DAY));
        oldCal.set(Calendar.MINUTE, newCal.get(Calendar.MINUTE));
        reminder = oldCal.getTime();
    }

    /**
     * Set the title to be saved when the task is modified or a new one is created.
     *
     * @param taskTitle task title
     */
    void setTaskTitle(String taskTitle) {
        editTaskFields.putTitle(taskTitle.trim());
    }

    /**
     * Set the notes to be saved when the task is modified or a new one is created.
     *
     * @param taskNotes task notes
     */
    void setTaskNotes(String taskNotes) {
        editTaskFields.putNotes(taskNotes.trim());
    }

    /**
     * Create a new task in the specified task list and include the fields inserted by the user.
     * The task is first created locally and then synced online if there is an active network connection.
     * If that's not the case, the task will be synced on the next refresh.
     *
     * @param taskListId task list identifier
     * @param isOnline   {@code true} if there is an active network connection
     */
    void newTask(String taskListId, boolean isOnline) {
        // Nothing entered
        if (editTaskFields.isEmpty()) {
            final EditTaskView view = view();
            if (view != null) view.onTaskSaved(null);
            return;
        }
        // Create the TTask offline
        Task task = new Task(prefHelper.getNextTaskId(), editTaskFields);
        TTask tTask = new TTask(task, taskListId);
        tTask.setSynced(false);
        tTask.setReminder(reminder);
        realm.executeTransaction(realm -> realm.copyToRealm(tTask));

        // Schedule the notification
        final TTask managedTask = tasksHelper.getTask(tTask.getId(), realm).toBlocking().first();
        notificationHelper.scheduleTaskNotification(managedTask);

        // Save the task on an active network connection
        if (isOnline) {
            tasksHelper.newTask(taskListId, editTaskFields)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            savedTask -> {
                                // Update the local task with the full information and delete the old task
                                final int id = managedTask.hashCode();
                                realm.executeTransaction(realm -> {
                                    managedTask.getTask().deleteFromRealm();
                                    managedTask.switchTask(realm.copyToRealm(savedTask));
                                    managedTask.setSynced(true);
                                });
                                Timber.d("new task with id %s", managedTask.getId());
                                prefHelper.deleteLastTaskId();
                                widgetHelper.updateWidgets(taskListId);
                                // Update the previous notification with the correct task ID
                                notificationHelper.scheduleTaskNotification(managedTask, id);
                            },
                            throwable -> {
                                Timber.e(throwable.toString());
                                final EditTaskView view = view();
                                if (view != null) view.onTaskSaveError();
                            }
                    );
        }
        widgetHelper.updateWidgets(taskListId);
        final EditTaskView view = view();
        if (view != null) view.onTaskSaved(tTask);
    }

    /**
     * Modify the specified task using the fields changed by the user. The task is
     * first updated locally and then, if an active network connection is available,
     * it is updated on the server.
     *
     * @param taskListId task list identifier
     * @param taskId     task identifier
     * @param isOnline   {@code true} if there is an active network connection
     */
    void updateTask(String taskListId, String taskId, boolean isOnline) {
        // No changes, return
        if (editTaskFields.isEmpty()) {
            final EditTaskView view = view();
            if (view != null) view.onTaskSaved(null);
            return;
        }
        // Update the task locally
        TTask managedTask = tasksHelper.getTask(taskId, realm).toBlocking().first();
        realm.executeTransaction(realm -> {
            managedTask.update(editTaskFields);
            managedTask.setReminder(reminder);
            managedTask.setSynced(false);
        });

        widgetHelper.updateWidgets(taskListId);
        notificationHelper.scheduleTaskNotification(managedTask);

        // Update the task on an active network connection
        if (isOnline) {
            tasksHelper.updateTask(taskListId, taskId, editTaskFields)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            task -> {
                                realm.executeTransaction(realm -> {
                                    realm.insertOrUpdate(task);
                                    managedTask.setSynced(true);
                                });
                                widgetHelper.updateWidgets(taskListId);
                            },
                            throwable -> {
                                Timber.e(throwable.toString());
                                final EditTaskView view = view();
                                if (view != null) view.onTaskSaveError();
                            }
                    );
        }
        final EditTaskView view = view();
        if (view != null) view.onTaskSaved(managedTask);
    }

    /**
     * Check if the due date is set.
     *
     * @return {@code true} if the due date is set, {@code false} otherwise
     */
    boolean hasDueDate() {
        return dueDate != null;
    }

    /**
     * Remove the due date.
     * The reminder date cannot exist without it so it is removed as well.
     */
    void removeDueDate() {
        dueDate = null;
        reminder = null;
    }

    @Override
    public void bindView(@NonNull EditTaskView view) {
        super.bindView(view);
        realm = Realm.getDefaultInstance();
    }

    @Override
    public void unbindView(@NonNull EditTaskView view) {
        super.unbindView(view);
        realm.close();
    }
}
