package com.teo.ttasks.data.model

import com.google.gson.annotations.Expose
import io.realm.RealmObject
import io.realm.annotations.Index
import io.realm.annotations.PrimaryKey
import java.util.*

open class Task : RealmObject {

    @Expose
    @PrimaryKey
    lateinit var id: String

    @Index
    lateinit var taskListId: String

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
    var hidden: Boolean = false

    /***** Custom fields *****/

    var reminder: Date? = null

    /**
     * Field indicating whether the task is synced and up-to-date with the server.
     * This is used to keep track of tasks updated locally but while offline.
     */
    var synced = true

    /**
     * Flag indicating whether the task was marked as deleted or not.
     * If true, the task doesn't appear in any list and it will be deleted from the server
     * at the next sync.
     */
    var deleted = false

    /**
     * Flag indicating whether a reminder notification was posted and dismissed for this task. This is used to avoid
     * posting more than one notification for a task or showing the notification again after the user
     * has dismissed it.
     *
     * **Note:** This flag should be reset whenever the reminder date changes.
     */
    var notificationDismissed = false

    var notificationId = 0
        private set

    val hasNotes: Boolean
        get() = !notes.isNullOrEmpty()

    val isCompleted: Boolean
        get() = completed != null

    /**
     * Check if this task is only available locally.
     *
     * @return `true` if it is local, `false` if it is also available on Google's servers
     */
    val isLocalOnly: Boolean
        get() {
            // If the id contains dashes, it's a UUID = local only
            return id.contains("-")
        }

    constructor()

    /**
     * Copy constructor. Used when updating a local task with a valid ID returned by the Google API.
     * Realm does not allow changing the primary key after an object was created so a new task must
     * be created with the current data and a new ID.
     */
    constructor(task: Task) {
        id = task.id
        kind = task.kind
        taskListId = task.taskListId
        reminder = task.reminder
        synced = task.synced
        deleted = task.deleted
        notificationDismissed = task.notificationDismissed
        notificationId = task.notificationId
    }

    /**
     * Create a new Task locally.
     * The ID needs to be unique so it doesn't conflict with other tasks.
     *
     * @param id         task identifier
     * @param taskListId the task list to which this task belongs
     */
    constructor(id: String, taskListId: String) {
        this.id = id
        this.taskListId = taskListId
    }

    companion object {
        const val STATUS_COMPLETED = "completed"
        const val STATUS_NEEDS_ACTION = "needsAction"
    }

    fun assignNotificationId() {
        notificationId = System.currentTimeMillis().hashCode()
    }

    fun copyCustomAttributes(task: Task) {
        reminder = task.reminder
        synced = task.synced
        deleted = task.deleted
        notificationDismissed = task.notificationDismissed
        notificationId = task.notificationId
    }
}
