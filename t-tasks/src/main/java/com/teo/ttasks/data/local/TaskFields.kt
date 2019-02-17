package com.teo.ttasks.data.local

import com.evernote.android.job.util.support.PersistableBundleCompat
import com.teo.ttasks.data.model.Task
import java.util.*

private const val NUM_FIELDS = 5

// TODO: remove this, it's unnecessary
class TaskFields : HashMap<String, String?>(NUM_FIELDS) {

    companion object {
        private const val KEY_TITLE = "title"
        private const val KEY_DUE = "due"
        private const val KEY_NOTES = "notes"
        private const val KEY_COMPLETED = "completed"
        private const val KEY_STATUS = "status"

        fun taskFields(block: TaskFields.() -> Unit): TaskFields = TaskFields().apply(block)

        /**
         * Convert bundle date to TaskFields. This is used when de-serializing task data
         * in a job
         */
        fun fromBundle(bundle: PersistableBundleCompat): TaskFields? {
            val taskFields = taskFields {
                if (bundle.containsKey(KEY_TITLE)) {
                    title = bundle[KEY_TITLE] as String
                }
                if (bundle.containsKey(KEY_DUE)) {
                    dueDate = bundle[KEY_DUE] as String
                }
                if (bundle.containsKey(KEY_NOTES)) {
                    notes = bundle[KEY_NOTES] as String
                }
                if (bundle.containsKey(KEY_COMPLETED)) {
                    completed = bundle[KEY_COMPLETED] as String?
                }
                if (bundle.containsKey(KEY_STATUS)) {
                    this[KEY_STATUS] = bundle[KEY_STATUS] as String
                }
            }
            return if (taskFields.isNotEmpty()) taskFields else null
        }
    }

    var title
        get() = this[KEY_TITLE]
        set(title) {
            when {
                // Disallow tasks with empty or no title
                title == null || title.isBlank() -> remove(KEY_TITLE)
                else -> this[KEY_TITLE] = title
            }
        }

    /**
     * The task's due date, always in UTC.
     */
    var dueDate
        get() = this[KEY_DUE]
        set(dueDate) {
            when (dueDate) {
                null -> remove(KEY_DUE)
                else -> this[KEY_DUE] = dueDate
            }
        }

    var notes
        get() = this[KEY_NOTES]
        set(notes) {
            when {
                notes == null || notes.isBlank() -> remove(KEY_NOTES)
                else -> this[KEY_NOTES] = notes
            }
        }

    var completed
        get() = this[KEY_COMPLETED]
        set(completed) {
            this[KEY_COMPLETED] = completed
            this[KEY_STATUS] = if (completed == null) Task.STATUS_NEEDS_ACTION else Task.STATUS_COMPLETED
        }
}
