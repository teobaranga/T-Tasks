package com.teo.ttasks.data.model;

import android.support.annotation.Nullable;

import com.google.api.client.util.DateTime;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import io.realm.Realm;
import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Setter;

@Data
@EqualsAndHashCode(callSuper = true)
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
    @Nullable
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

    /** Notes describing the task. */
    @Nullable
    private String notes;

    /** Status of the task. This is either "needsAction" or "completed". */
    private String status;

    /**
     * Due date of the task.<br>
     * This is not a completely correct date, see link below.
     *
     * @see <a href="https://groups.google.com/forum/#!topic/google-tasks-api/sDJo6ohfPQU">
     * Due date is not a calculated field</a>
     */
    @Nullable
    @Setter(AccessLevel.NONE)
    private Date due;

    /**
     * Completion date of the task. This field is omitted if the task has not
     * been completed.
     */
    @Nullable
    @Setter(AccessLevel.NONE)
    private Date completed;

    private String taskListId;

    @Nullable
    private Date reminder;

    public static Task create(com.google.api.services.tasks.model.Task task, String taskListId, Realm realm) {
        Task t = realm.createObjectFromJson(Task.class, task.toString());
        t.setTaskListId(taskListId);
        t.setDue(task.getDue());
        t.setCompleted(task.getCompleted());
        return t;
    }

    public void setDue(@Nullable DateTime due) {
        if (due == null) {
            this.due = null;
            return;
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeZone(TimeZone.getTimeZone("UTC"));
        GregorianCalendar gregorianCalendar = new GregorianCalendar();
        calendar.setTime(new Date(due.getValue()));
        gregorianCalendar.set(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH), 0, 0, 0);
        this.due = gregorianCalendar.getTime();
    }

    public void setCompleted(@Nullable DateTime completed) {
        if (completed == null) {
            this.completed = null;
            return;
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeZone(TimeZone.getTimeZone("UTC"));
        GregorianCalendar gregorianCalendar = new GregorianCalendar();
        calendar.setTime(new Date(completed.getValue()));
        gregorianCalendar.set(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH), 0, 0, 0);
        this.completed = gregorianCalendar.getTime();
    }
}
