package com.teo.ttasks.data.local

import com.teo.ttasks.data.model.Task
import java.util.*

class TaskFields : HashMap<String, Any>() {

    companion object {
        private const val KEY_TITLE = "title"
        private const val KEY_DUE = "due"
        private const val KEY_NOTES = "notes"
        private const val KEY_COMPLETED = "completed"
        private const val KEY_STATUS = "status"
    }

    var title: String?
        get() = get(KEY_TITLE) as? String
        set(title) = title.let { if (it == null || it.isBlank()) remove(KEY_TITLE) else put(KEY_TITLE, it) }

    var dueDate: Date?
        get() = get(KEY_DUE) as? Date
        set(dueDate) = dueDate.let { if (it == null) remove(KEY_DUE) else put(KEY_DUE, it) }

    var notes: String?
        get() = get(KEY_NOTES) as? String
        set(notes) = notes.let { if (it == null || it.isBlank()) remove(KEY_NOTES) else put(KEY_NOTES, it) }

    fun putCompleted(isCompleted: Boolean, completed: Date?) {
        put(KEY_STATUS, if (isCompleted) Task.STATUS_COMPLETED else Task.STATUS_NEEDS_ACTION)
        if (completed == null) remove(KEY_COMPLETED) else put(KEY_COMPLETED, completed)
    }
}
