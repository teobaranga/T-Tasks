package com.teo.ttasks.data.model

import com.teo.ttasks.data.local.TaskListFields
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey

/**
 * Must be created only with the overloaded constructor.
 */
open class TTaskList : RealmObject {

    @PrimaryKey
    lateinit var id: String

    private var _taskList: TaskList? = null

    var taskList: TaskList
        get() = _taskList!!
        set(value) {
            _taskList = value
        }

    var title: String
        get() = taskList.title
        set(title) {
            taskList.title = title
        }

    /**
     * Field indicating whether the task list is synced and up-to-date with the server.
     * This is used to keep track of task lists updated locally but while offline.
     */
    var synced = true

    var deleted = false

    constructor()

    constructor(taskList: TaskList) : super() {
        this.taskList = taskList
        this.id = taskList.id
    }

    /**
     * Update the task with the specified fields.
     * Requires to executed in a Realm transaction.

     * @param taskListFields fields to be updated
     */
    fun update(taskListFields: TaskListFields) {
        taskList.title = taskListFields.title!!
    }

    fun switchTaskList(taskList: TaskList) {
        this.taskList = taskList
        this.id = taskList.id
    }
}
