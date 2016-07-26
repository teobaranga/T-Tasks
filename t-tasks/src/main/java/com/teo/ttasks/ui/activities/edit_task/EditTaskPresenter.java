package com.teo.ttasks.ui.activities.edit_task;

import android.support.annotation.NonNull;

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

    private final TasksHelper mTasksHelper;

    private String mTaskTitle;
    private Date mDateDue;
    private String mTaskNotes;

    private Realm mRealm;

    public EditTaskPresenter(TasksHelper tasksHelper) {
        mTasksHelper = tasksHelper;
    }

    void loadTaskInfo(String taskId, String taskListId) {
        mTasksHelper.getTask(taskId, mRealm)
                .doOnNext(task -> {
                    final EditTaskView view = view();
                    if (view != null) view.onTaskLoaded(task);
                })
                .flatMap(ignored -> mTasksHelper.getTaskList(taskListId, mRealm))
                .doOnNext(taskList -> {
                    final EditTaskView view = view();
                    if (view != null) view.onTaskListLoaded(taskList);
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
        mTasksHelper.getTaskLists(mRealm)
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
        if (mDateDue == null) {
            mDateDue = date;
        } else {
            Timber.d("old date %s", mDateDue.toString());
            Calendar oldCal = Calendar.getInstance();
            oldCal.setTime(mDateDue);

            Calendar newCal = Calendar.getInstance();
            newCal.setTime(date);

            oldCal.set(newCal.get(Calendar.YEAR), newCal.get(Calendar.MONTH), newCal.get(Calendar.DAY_OF_MONTH));
            mDateDue = oldCal.getTime();
            Timber.d("new date %s", mDateDue.toString());
        }
    }

    void setDueTime(Date date) {
        if (mDateDue == null) {
            mDateDue = date;
        } else {
            Timber.d("old date %s", mDateDue.toString());
            Calendar oldCal = Calendar.getInstance();
            oldCal.setTime(mDateDue);

            Calendar newCal = Calendar.getInstance();
            newCal.setTime(date);

            oldCal.set(Calendar.HOUR_OF_DAY, newCal.get(Calendar.HOUR_OF_DAY));
            oldCal.set(Calendar.MINUTE, newCal.get(Calendar.MINUTE));
            mDateDue = oldCal.getTime();
            Timber.d("new date %s", mDateDue.toString());
        }
    }

    void setTaskTitle(String taskTitle) {
        mTaskTitle = taskTitle;
        Timber.d("Title: %s", mTaskTitle);
    }

    public void setTaskNotes(String taskNotes) {
        mTaskNotes = taskNotes;
    }

    void saveTask(String taskListId) {
        HashMap<String, Object> newTask = new HashMap<>();
        newTask.put("title", mTaskTitle);
        newTask.put("due", mDateDue);
        newTask.put("notes", mTaskNotes);
        mTasksHelper.newTask(taskListId, newTask)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        task -> {
                            mRealm.executeTransaction(realm1 -> realm1.copyToRealmOrUpdate(task));
                            final EditTaskView view = view();
                            if (view != null) view.onTaskSaved();
                        },
                        throwable -> {
                            Timber.e(throwable.toString());
                            final EditTaskView view = view();
                            if (view != null) view.onTaskSaveError();
                        }
                );
    }

    @Override
    public void bindView(@NonNull EditTaskView view) {
        super.bindView(view);
        mRealm = Realm.getDefaultInstance();
    }

    @Override
    public void unbindView(@NonNull EditTaskView view) {
        super.unbindView(view);
        mRealm.close();
    }
}
