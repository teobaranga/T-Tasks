package com.teo.ttasks.data.local;

import android.support.annotation.Nullable;

import com.teo.ttasks.data.model.TTask;

import java.util.Date;
import java.util.HashMap;

import static com.teo.ttasks.data.model.Task.STATUS_COMPLETED;
import static com.teo.ttasks.data.model.Task.STATUS_NEEDS_ACTION;

public class TaskFields extends HashMap<String, Object> {

    private static final String KEY_TITLE = "title";
    private static final String KEY_DUE = "due";
    private static final String KEY_NOTES = "notes";
    private static final String KEY_COMPLETED = "completed";
    private static final String KEY_STATUS = "status";

    public static TaskFields fromTask(TTask task) {
        final TaskFields taskFields = new TaskFields();
        taskFields.putTitle(task.getTitle());
        taskFields.putCompleted(task.isCompleted(), task.getCompleted());
        taskFields.putDueDate(task.getDue());
        taskFields.putNotes(task.getNotes());
        return taskFields;
    }

    public void putTitle(@Nullable String title) {
        if (title == null || title.isEmpty()) {
            remove(KEY_TITLE);
            return;
        }
        put(KEY_TITLE, title);
    }

    public void putDueDate(@Nullable Date dueDate) {
        if (dueDate == null) {
            remove(KEY_DUE);
            return;
        }
        put(KEY_DUE, dueDate);
    }

    public void putNotes(@Nullable String notes) {
        put(KEY_NOTES, notes);
    }

    public void putCompleted(boolean isCompleted, @Nullable Date completed) {
        if (isCompleted) {
            put(KEY_STATUS, STATUS_COMPLETED);
        } else {
            put(KEY_STATUS, STATUS_NEEDS_ACTION);
        }
        put(KEY_COMPLETED, completed);
    }

    public String getTitle() {
        return (String) get(KEY_TITLE);
    }

    public Date getDueDate() {
        return (Date) get(KEY_DUE);
    }

    public String getNotes() {
        return (String) get(KEY_NOTES);
    }
}
