package com.teo.ttasks.data.model

import com.google.gson.annotations.Expose
import com.teo.ttasks.data.local.TaskListFields
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey

open class TaskList : RealmObject() {

    @Expose
    @PrimaryKey
    lateinit var id: String

    @Expose
    lateinit var title: String

    @Expose
    var updated: String? = null

    /***** Custom attributes *****/

    /**
     * Field indicating whether the task list is synced and up-to-date with the server.
     * This is used to keep track of task lists updated locally but while offline.
     */
    var synced = true

    var deleted = false

    /**
     * Update the task with the specified fields.
     * Requires to executed in a Realm transaction.

     * @param taskListFields fields to be updated
     */
    fun update(taskListFields: TaskListFields) {
        title = taskListFields.title!!
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TaskList

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}
