package com.teo.ttasks.data.model;

import com.teo.ttasks.data.local.TaskListFields;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Must be created only with the overloaded constructor.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class TTaskList extends RealmObject {

    @PrimaryKey
    public String id;

    public TaskList taskList;

    /**
     * Field indicating whether the task list is synced and up-to-date with the server.
     * This is used to keep track of task lists updated locally but while offline.
     */
    private boolean synced = true;

    private boolean deleted = false;

    public TTaskList() { }

    public TTaskList(TaskList taskList) {
        this.taskList = taskList;
        this.id = taskList.getId();
    }

    /**
     * Update the task with the specified fields.
     * Requires to executed in a Realm transaction.
     *
     * @param taskListFields fields to be updated
     */
    public void update(TaskListFields taskListFields) {
        String title = taskListFields.getTitle();
        if (title != null)
            taskList.setTitle(title);
    }

    public void switchTaskList(TaskList taskList) {
        this.taskList = taskList;
        this.id = taskList.getId();
    }

    public String getTitle() {
        return taskList.getTitle();
    }

    public void setTitle(String title) {
        taskList.setTitle(title);
    }
}
