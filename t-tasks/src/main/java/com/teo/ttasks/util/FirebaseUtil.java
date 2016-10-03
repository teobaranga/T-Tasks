package com.teo.ttasks.util;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class FirebaseUtil {

    private FirebaseUtil() { }

    public static DatabaseReference getTasksDatabase() {
        return FirebaseDatabase.getInstance().getReference("tasks");
    }

    public static void saveReminder(DatabaseReference databaseReference, String taskId, Long dateInMillis) {
        databaseReference.child(taskId).child("reminder").setValue(dateInMillis);
    }
}
