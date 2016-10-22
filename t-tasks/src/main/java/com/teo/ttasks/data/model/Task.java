package com.teo.ttasks.data.model;

import android.support.annotation.Nullable;

import com.google.gson.annotations.Expose;
import com.teo.ttasks.data.local.TaskFields;

import java.util.Date;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Setter;

@Data
@EqualsAndHashCode(callSuper = true)
public class Task extends RealmObject {

    public static final String STATUS_COMPLETED = "completed";
    public static final String STATUS_NEEDS_ACTION = "needsAction";

    @Expose
    @PrimaryKey
    private String id;

    @Expose(deserialize = false)
    @Setter(AccessLevel.NONE)
    private String kind = "tasks#task";

    @Expose
    private String etag;

    @Expose
    private String title;

    @Expose
    private Date updated;

    @Expose
    private String selfLink;

    /**
     * Parent task identifier. This field is omitted if it is a top-level task.
     * This field is read-only. Use the "move" method to move the task under a
     * different parent or to the top level.
     */
    @Expose
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
    @Expose
    private String position;

    /** Notes describing the task. */
    @Expose
    @Nullable
    private String notes;

    /** Status of the task. This is either "needsAction" or "completed". */
    @Expose
    private String status;

    /**
     * Due date of the task.<br>
     * This is not a completely correct date, see link below.
     *
     * @see <a href="https://groups.google.com/forum/#!topic/google-tasks-api/sDJo6ohfPQU">
     * Due date is not a calculated field</a>
     */
    @Expose
    @Nullable
    private Date due;

    /**
     * Completion date of the task. This field is omitted if the task has not
     * been completed.
     */
    @Expose
    @Nullable
    private Date completed;

    @Expose
    private boolean deleted;

    @Expose
    private boolean hidden;

    public Task() { }

    /**
     * Create a new Task locally.<br>
     * The ID needs to be unique so it doesn't conflict with other tasks.
     *
     * @param id         task identifier
     * @param taskFields the task information
     */
    public Task(String id, TaskFields taskFields) {
        this.id = id;
        title = taskFields.getTitle();
        due = taskFields.getDueDate();
        notes = taskFields.getNotes();
    }
}
