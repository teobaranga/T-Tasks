package com.teo.ttasks.data.model;

import android.support.annotation.Nullable;

import java.util.Date;

import io.realm.RealmObject;
import io.realm.annotations.Index;
import io.realm.annotations.PrimaryKey;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Must be created only with the overloaded constructor
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class TTask extends RealmObject {

    @PrimaryKey
    public String id;

    @Index
    public String taskListId;

    public Task task;

    @Nullable
    private Date reminder;

    /**
     * Field indicating whether the task is synced and up-to-date with the server.
     * This is used to keep track of tasks updated locally but while offline.
     */
    private boolean synced = true;

    /**
     * Flag indicating whether the task was marked as deleted or not.
     * If true, the task doesn't appear in any list and it will be deleted from the server
     * at the next sync.
     */
    private boolean deleted = false;

    /**
     * Flag indicating whether a reminder notification was posted and dismissed for this task. This is used to avoid
     * posting more than one notification for a task or showing the notification again after the user
     * has dismissed it.
     * <p>
     * <b>Note:</b> This flag should be reset whenever the reminder date changes.
     */
    private boolean notificationDismissed = false;

    public TTask() { }

    /**
     * Copy constructor. Used when updating a local task with a valid ID returned by the Google API.
     * Realm does not allow changing the primary key after an object was created so a new task must
     * be created with the current data and a new ID.
     *
     * @param tTask
     * @param task
     */
    public TTask(TTask tTask, Task task) {
        this.task = task;
        id = task.getId();
        taskListId = tTask.getTaskListId();
        reminder = tTask.getReminder();
        synced = tTask.isSynced();
        deleted = tTask.isDeleted();
    }

    public TTask(Task task, String taskListId) {
        this.task = task;
        this.id = task.getId();
        this.taskListId = taskListId;
    }

    /**
     * Update the task with the specified fields.
     * Requires to executed in a Realm transaction.
     *
     * @param taskFields fields to be updated
     */
    public void update(TaskFields taskFields) {
        String title = taskFields.getTitle();
        String notes = taskFields.getNotes();
        Date due = taskFields.getDueDate();
        if (title != null)
            task.setTitle(title);
        if (notes != null)
            task.setNotes(notes);
        if (due != null)
            task.setDue(due);
    }

    public String getTitle() {
        return task.getTitle();
    }

    public void setTitle(String title) {
        task.setTitle(title);
    }

    @Nullable
    public String getNotes() {
        return task.getNotes();
    }

    public boolean hasNotes() {
        final String notes = getNotes();
        return notes != null && !notes.isEmpty();
    }

    @Nullable
    public Date getDue() {
        return task.getDue();
    }

    public boolean isCompleted() {
        return task.getCompleted() != null;
    }

    @Nullable
    public Date getCompleted() {
        return task.getCompleted();
    }

    public void setCompleted(Date completed) {
        task.setCompleted(completed);
    }

    public String getStatus() {
        return task.getStatus();
    }

    public void setStatus(String status) {
        task.setStatus(status);
    }

    @Nullable
    public Date getReminder() {
        return reminder;
    }

    public boolean isNew() {
        try {
            //noinspection ResultOfMethodCallIgnored
            Integer.parseInt(id);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
