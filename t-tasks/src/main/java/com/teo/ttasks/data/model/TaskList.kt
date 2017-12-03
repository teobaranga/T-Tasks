package com.teo.ttasks.data.model

import com.google.gson.annotations.Expose
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey

open class TaskList : RealmObject() {

    @Expose
    @PrimaryKey
    lateinit var id: String

    @Expose
    lateinit var title: String

    @Expose
    var kind: String? = null

    @Expose
    var selfLink: String? = null

    @Expose
    var updated: String? = null
}
