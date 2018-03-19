package com.teo.ttasks.api.entities

import com.google.gson.annotations.Expose
import com.teo.ttasks.data.model.Task

import io.realm.RealmList
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey

open class TasksResponse : RealmObject() {

    companion object {
        val EMPTY: TasksResponse = TasksResponse()
    }

    @Expose
    @PrimaryKey
    var id: String? = null

    @Expose
    var etag: String? = null

    @Expose
    var nextPageToken: String? = null

    @Expose
    var items: RealmList<Task>? = null
}
