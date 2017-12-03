package com.teo.ttasks.api.entities

import com.google.gson.annotations.Expose
import com.teo.ttasks.data.model.TaskList

import io.realm.RealmList
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey

open class TaskListsResponse : RealmObject() {

    /**
     * The ID of this TaskListResponse, which is the email of the user
     */
    @Expose
    @PrimaryKey
    lateinit var id: String

    @Expose
    lateinit var etag: String

    @Expose
    var items: RealmList<TaskList>? = null
}
