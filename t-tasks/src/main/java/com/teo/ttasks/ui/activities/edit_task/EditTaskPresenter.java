package com.teo.ttasks.ui.activities.edit_task;

import android.support.annotation.NonNull;

import com.teo.ttasks.data.model.TTask;
import com.teo.ttasks.data.model.TaskList;
import com.teo.ttasks.data.remote.TasksHelper;
import com.teo.ttasks.ui.base.Presenter;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

import io.realm.Realm;
import rx.android.schedulers.AndroidSchedulers;
import timber.log.Timber;

public class EditTaskPresenter extends Presenter<EditTaskView> {

    private final TasksHelper tasksHelper;

    private String taskTitle;
    private Date dueDate;
    private Date reminder;
    private String notes;

    private Realm realm;

    public EditTaskPresenter(TasksHelper tasksHelper) {
        this.tasksHelper = tasksHelper;
    }

    void loadTaskInfo(String taskId) {
        tasksHelper.getTask(taskId, realm)
                .doOnNext(task -> {
                    taskTitle = task.getTitle();
                    dueDate = task.getDue();
                    reminder = task.getReminder();
                    notes = task.getNotes();
                    final EditTaskView view = view();
                    if (view != null) view.onTaskLoaded(task);
                })
                .subscribe(
                        ignored -> { },
                        throwable -> {
                            Timber.e(throwable.toString());
                            final EditTaskView view = view();
                            if (view != null) view.onTaskInfoError();
                        }
                );

    }

    void loadTaskLists(String currentTaskListId) {
        Timber.d("loading task lists, %s", currentTaskListId);
        tasksHelper.getTaskLists(realm)
                .subscribe(
                        taskLists -> {
                            for (int i = 0; i < taskLists.size(); i++) {
                                TaskList taskList = taskLists.get(i);
                                if (taskList.getId().equals(currentTaskListId)) {
                                    final EditTaskView view = view();
                                    if (view != null) view.onTaskListsLoaded(taskLists, i);
                                    return;
                                }
                            }
                        },
                        throwable -> {
                            Timber.e(throwable.toString());
                            // TODO: 2016-07-24 implement error
                            final EditTaskView view = view();
                            if (view != null) view.onTaskInfoError();
                        }
                );
    }

    /**
     * Set the due date. If one isn't present, assign the new one. Otherwise, modify the old one.
     *
     * @param date the due date
     */
    void setDueDate(Date date) {
        if (dueDate == null) {
            dueDate = date;
        } else {
            Timber.d("old date %s", dueDate.toString());
            Calendar oldCal = Calendar.getInstance();
            oldCal.setTime(dueDate);

            Calendar newCal = Calendar.getInstance();
            newCal.setTime(date);

            oldCal.set(newCal.get(Calendar.YEAR), newCal.get(Calendar.MONTH), newCal.get(Calendar.DAY_OF_MONTH));
            dueDate = oldCal.getTime();
            Timber.d("new date %s", dueDate.toString());
        }
    }

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

    void setTaskTitle(String taskTitle) {
        this.taskTitle = taskTitle;
        Timber.d("Title: %s", this.taskTitle);
    }

    public void setTaskNotes(String taskNotes) {
        notes = taskNotes;
    }

    void newTask(String taskListId) {
        HashMap<String, Object> newTask = new HashMap<>();
        newTask.put("title", taskTitle);
        newTask.put("due", dueDate);
        newTask.put("notes", notes);
        // TODO: 2016-08-09 save the reminder online
        tasksHelper.newTask(taskListId, newTask)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        task -> {
                            // Create the TTask, set the reminder and save it to Realm
                            TTask tTask = new TTask(task, taskListId);
                            tTask.setReminder(reminder);
                            realm.executeTransaction(realm -> realm.copyToRealmOrUpdate(tTask));
                            final EditTaskView view = view();
                            if (view != null) view.onTaskSaved(tTask);
                        },
                        throwable -> {
                            Timber.e(throwable.toString());
                            final EditTaskView view = view();
                            if (view != null) view.onTaskSaveError();
                        }
                );
    }

    // TODO: 2016-07-28 finish this
    void updateTask(String taskListId, String taskId) {
        HashMap<String, Object> taskFields = new HashMap<>();
        tasksHelper.updateTask(taskListId, taskId, taskFields)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        task -> {
                            realm.executeTransaction(realm -> realm.copyToRealmOrUpdate(task));
                            final EditTaskView view = view();
//                            if (view != null) view.onTaskSaved(task);
                        },
                        throwable -> {
                            Timber.e(throwable.toString());
                            final EditTaskView view = view();
                            if (view != null) view.onTaskSaveError();
                        }
                );
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
