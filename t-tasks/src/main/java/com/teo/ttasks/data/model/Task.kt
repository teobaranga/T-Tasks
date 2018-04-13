package com.teo.ttasks.data.model

import com.google.gson.annotations.Expose
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import java.util.*

open class Task : RealmObject {

    @Expose
    @PrimaryKey
    lateinit var id: String

    @Expose(deserialize = false)
    var kind = "tasks#task"
        private set

    @Expose
    var etag: String? = null

    @Expose
    var title: String? = null

    @Expose
    var updated: Date? = null

    /**
     * Parent task identifier. This field is omitted if it is a top-level task.
     * This field is read-only. Use the "move" method to move the task under a
     * different parent or to the top level.
     */
    @Expose
    var parent: String? = null

    /**
     * String indicating the position of the task among its sibling tasks under
     * the same parent task or at the top level. If this string is greater than
     * another task's corresponding position string according to lexicographical
     * ordering, the task is positioned after the other task under the same parent
     * task (or at the top level). This field is read-only. Use the "move" method
     * to move the task to another position.
     */
    @Expose
    var position: String? = null

    /** Notes describing the task. */
    @Expose
    var notes: String? = null

    /** Status of the task. This is either "needsAction" or "completed". */
    @Expose
    var status: String? = null

    /**
     * Due date of the task
     */
    @Expose
    var due: Date? = null

    /**
     * Completion date of the task. This field is omitted if the task has not
     * been completed.
     */
    @Expose
    var completed: Date? = null

    @Expose
    var deleted: Boolean = false

    @Expose
    var hidden: Boolean = false

    constructor()

    /**
     * Create a new Task locally.
     * The ID needs to be unique so it doesn't conflict with other tasks.
     *
     * @param id task identifier
     */
    constructor(id: String) {
        this.id = id
    }

    companion object {
        const val STATUS_COMPLETED = "completed"
        const val STATUS_NEEDS_ACTION = "needsAction"
    }
}
