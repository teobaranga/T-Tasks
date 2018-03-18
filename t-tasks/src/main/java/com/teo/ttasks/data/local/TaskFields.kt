package com.teo.ttasks.data.local

import com.teo.ttasks.data.model.Task
import java.util.*

private const val NUM_FIELDS = 5

class TaskFields : HashMap<String, Any>(NUM_FIELDS) {

    companion object {
        private const val KEY_TITLE = "title"
        private const val KEY_DUE = "due"
        private const val KEY_NOTES = "notes"
        private const val KEY_COMPLETED = "completed"
        private const val KEY_STATUS = "status"
    }

    var title: String?
        get() = this[KEY_TITLE] as? String
        set(title) {
            when {
            // Disallow tasks with empty or no title
                title == null || title.isBlank() -> remove(KEY_TITLE)
                else -> this[KEY_TITLE] = title
            }
        }

    var dueDate: Date?
        get() = this[KEY_DUE] as? Date
        set(dueDate) {
            when (dueDate) {
                null -> remove(KEY_DUE)
                else -> this[KEY_DUE] = dueDate
            }
        }

    var notes: String?
        get() = this[KEY_NOTES] as? String
        set(notes) {
            when {
                notes == null || notes.isBlank() -> remove(KEY_NOTES)
                else -> this[KEY_NOTES] = notes
            }
        }

    fun putCompleted(isCompleted: Boolean, completed: Date?) {
        this[KEY_STATUS] = if (isCompleted) Task.STATUS_COMPLETED else Task.STATUS_NEEDS_ACTION
        when (completed) {
            null -> remove(KEY_COMPLETED)
            else -> this[KEY_COMPLETED] = completed
        }
    }
}
