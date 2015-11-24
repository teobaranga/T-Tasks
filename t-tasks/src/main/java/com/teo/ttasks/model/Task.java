package com.teo.ttasks.model;

import java.util.Date;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

public class Task extends RealmObject {

    @PrimaryKey
    private String id;
    private String title;
    private Date updatedDate;
    private String selfLink;

    /**
     * Parent task identifier. This field is omitted if it is a top-level task.
     * This field is read-only. Use the "move" method to move the task under a
     * different parent or to the top level.
     */
    private String parent;

    /**
     * String indicating the position of the task among its sibling tasks under
     * the same parent task or at the top level. If this string is greater than
     * another task's corresponding position string according to lexicographical
     * ordering, the task is positioned after the other task under the same parent
     * task (or at the top level). This field is read-only. Use the "move" method
     * to move the task to another position.
     */
    private String position;

    /** Notes describing the task. Optional. */
    private String notes;

    /** Status of the task. This is either "needsAction" or "completed". */
    private String status;

    /** Due date of the task. Optional. */
    private Date dueDate;

    /**
     * Completion date of the task. This field is omitted if the task has not
     * been completed.
     */
    private Date completedDate;
    private String taskListId;
    private Date reminder;

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getParent() {
        return parent;
    }

    public void setParent(String parent) {
        this.parent = parent;
    }

    public Date getDueDate() {
        return dueDate;
    }

    public void setDueDate(Date dueDate) {
        this.dueDate = dueDate;
    }

    public String getTaskListId() {
        return taskListId;
    }

    public void setTaskListId(String taskListId) {
        this.taskListId = taskListId;
    }

    public Date getReminder() {
        return reminder;
    }

    public void setReminder(Date reminder) {
        this.reminder = reminder;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Date getUpdatedDate() {
        return updatedDate;
    }

    public void setUpdatedDate(Date updatedDate) {
        this.updatedDate = updatedDate;
    }

    public String getSelfLink() {
        return selfLink;
    }

    public void setSelfLink(String selfLink) {
        this.selfLink = selfLink;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Date getCompletedDate() {
        return completedDate;
    }

    public void setCompletedDate(Date completedDate) {
        this.completedDate = completedDate;
    }
}
