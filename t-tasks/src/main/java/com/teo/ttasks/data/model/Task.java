package com.teo.ttasks.data.model;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

public class Task extends RealmObject {

    public static final String TASK_LIST_ID = "taskListId";

    @PrimaryKey
    private String id;
    private String title;
    private Date updated;
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

    /**
     * Due date of the task. Optional.<br>
     * This is not a completely correct date, see link below.
     * @see <a href="https://groups.google.com/forum/#!topic/google-tasks-api/sDJo6ohfPQU">
     *     Due date is not a calculated field</a>
     */
    private Date due;

    /**
     * Completion date of the task. This field is omitted if the task has not
     * been completed.
     */
    private Date completed;
    private String taskListId;
    private Date reminder;

    /**
     * Fixes the {@link Task#due} and {@link Task#completed} dates of the given {@link Task} by adjusting it to the current user's time zone.<br>
     * This is a separate method because Realm doesn't allow custom setters, see link below.
     * @see <a href="https://github.com/realm/realm-java/issues/909">
     *     Support adding own methods to objects #909 - Realm</a>
     */
    public static void fixDates(Task task) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeZone(TimeZone.getTimeZone("UTC"));
        GregorianCalendar gregorianCalendar = new GregorianCalendar();
        if (task.getDue() != null) {
            calendar.setTime(task.getDue());
            gregorianCalendar.set(calendar.get(Calendar.YEAR),calendar.get(Calendar.MONTH),calendar.get(Calendar.DAY_OF_MONTH), 0, 0, 0);
            task.setDue(gregorianCalendar.getTime());
        }
        if (task.getCompleted() != null) {
            calendar.setTime(task.getCompleted());
            gregorianCalendar.set(calendar.get(Calendar.YEAR),calendar.get(Calendar.MONTH),calendar.get(Calendar.DAY_OF_MONTH), 0, 0, 0);
            task.setCompleted(gregorianCalendar.getTime());
        }
    }

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

    public Date getDue() {
        return due;
    }

    public void setDue(Date due) {
        this.due = due;
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

    public Date getUpdated() {
        return updated;
    }

    public void setUpdated(Date updated) {
        this.updated = updated;
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

    public Date getCompleted() {
        return completed;
    }

    public void setCompleted(Date completed) {
        this.completed = completed;
    }
}
