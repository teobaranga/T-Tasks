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

    public TTask() { }

    public TTask(Task task, String taskListId) {
        this.task = task;
        this.id = task.getId();
        this.taskListId = taskListId;
    }

    @Nullable
    private Date reminder;

    /**
     * Field indicating whether the task is synced and up-to-date with the server.
     * This is used to keep track of tasks updated locally but while offline.
     */
    private boolean synced = true;

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

    @Nullable
    public Date getDue() {
        return task.getDue();
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
}
