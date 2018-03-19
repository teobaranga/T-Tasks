package com.teo.ttasks.util

import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import timber.log.Timber

object FirebaseUtil {

    private const val DB_TASKS = "tasks"

    /** The reference to the Firebase Database containing additional information about all the tasks */
    fun FirebaseDatabase.getTasksDatabase(): DatabaseReference {
        return this.getReference(DB_TASKS)
    }

    /**
     * Get a reference to the reminder property of the given task
     *
     * @param taskId ID of the task
     */
    fun DatabaseReference.reminder(taskId: String): DatabaseReference? {
        if (this.key != DB_TASKS) {
            Timber.w("Attempting to access task reminders in the wrong database")
            return null
        }
        return this.child(taskId).child("reminder")
    }

    /**
     * Set or clear the reminder date for a given task
     *
     * @param taskId ID of the modified task
     * @param dateInMillis the reminder date in milliseconds, can be null to remove the reminder
     */
    fun DatabaseReference.saveReminder(taskId: String, dateInMillis: Long?) {
        this.reminder(taskId)?.setValue(dateInMillis)
    }
}
