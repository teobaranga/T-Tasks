package com.teo.ttasks.api.entities

import com.google.gson.annotations.Expose
import com.teo.ttasks.data.model.Task

import io.realm.RealmList
import io.realm.RealmObject
import io.realm.annotations.Ignore
import io.realm.annotations.PrimaryKey

open class TasksResponse : RealmObject() {

    companion object {
        val EMPTY: TasksResponse = TasksResponse()
    }

    /** The task list ID */
    @Expose
    @PrimaryKey
    var id: String? = null

    @Expose
    var etag: String? = null

    @Expose
    @Ignore
    var nextPageToken: String? = null

    /**
     * The list of tasks associated with this task list.
     *
     * This list does not need to be persisted since each individual task will be persisted anyways.
     * */
    @Expose
    @Ignore
    var items: RealmList<Task>? = null
}
