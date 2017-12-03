package com.teo.ttasks.data.model

import com.teo.ttasks.data.local.TaskFields
import io.realm.RealmObject
import io.realm.annotations.Index
import io.realm.annotations.PrimaryKey
import java.util.*

/**
 * Must be created only with the overloaded constructor
 */
open class TTask : RealmObject {

    @PrimaryKey
    lateinit var id: String

    @Index
    lateinit var taskListId: String

    private var _task: Task? = null

    var task: Task
        get() = _task!!
        set(value) {
            _task = value
        }

    var reminder: Date? = null

    var title: String
        get() = task.title!!
        set(value) {
            task.title = value
        }

    var completed: Date?
        get() = task.completed
        set(value) {
            task.completed = value
        }

    var status: String?
        get() = task.status
        set(value) {
            task.status = value
        }

    val due: Date?
        get() = task.due

    val notes: String?
        get() = task.notes

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
     *
     * **Note:** This flag should be reset whenever the reminder date changes.
     */
    var notificationDismissed = false

    var notificationId = 0
        private set

    val hasNotes: Boolean
        get() = !notes.isNullOrEmpty()

    val isCompleted: Boolean
        get() = task.completed != null

    /**
     * Check if this task is only available locally.

     * @return `true` if it is local, `false` if it is also available on Google's servers
     */
    val isLocalOnly: Boolean
        get() {
            try {
                Integer.parseInt(id)
                return true
            } catch (e: NumberFormatException) {
                return false
            }

        }

    constructor()

    /**
     * Copy constructor. Used when updating a local task with a valid ID returned by the Google API.
     * Realm does not allow changing the primary key after an object was created so a new task must
     * be created with the current data and a new ID.
     */
    constructor(tTask: TTask, task: Task) {
        this.task = task
        id = task.id
        taskListId = tTask.taskListId
        reminder = tTask.reminder
        synced = tTask.synced
        deleted = tTask.deleted
        notificationDismissed = tTask.notificationDismissed
        notificationId = tTask.notificationId
    }

    constructor(task: Task, taskListId: String) {
        this.task = task
        this.id = task.id
        this.taskListId = taskListId
    }

    /**
     * Update the task with the specified fields.
     * Requires to executed in a Realm transaction.

     * @param taskFields fields to be updated
     */
    fun update(taskFields: TaskFields) {
        task.title = taskFields.title
        task.notes = taskFields.notes
        task.due = taskFields.dueDate
    }

    fun assignNotificationId() {
        notificationId = System.currentTimeMillis().hashCode()
    }
}
