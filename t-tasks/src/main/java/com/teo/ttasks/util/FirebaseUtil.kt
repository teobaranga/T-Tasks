package com.teo.ttasks.util

import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

object FirebaseUtil {

    private val tasksDatabase: DatabaseReference
        get() = FirebaseDatabase.getInstance().getReference("tasks")

    fun saveReminder(taskId: String, dateInMillis: Long?) {
        tasksDatabase.child(taskId).child("reminder").setValue(dateInMillis)
    }
}
