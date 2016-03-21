package com.teo.ttasks.data.local;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

public class PrefHelper {

    private static final String PREF_USER_EMAIL = "email",
            PREF_USER_NAME = "name",
            PREF_USER_PHOTO = "photo";
    private static final String PREF_CURRENT_TASK_LIST_ID = "currentTaskListId";

    public static void updateCurrentTaskList(Context context, String currentTaskListId) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        preferences.edit().putString(PREF_CURRENT_TASK_LIST_ID, currentTaskListId).apply();
    }

    @Nullable
    public static String getCurrentTaskList(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return preferences.getString(PREF_CURRENT_TASK_LIST_ID, null);
    }

    public static boolean isUserPresent(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return preferences.getString(PREF_USER_EMAIL, null) != null;
    }

    public static void setUser(Context context, String email, String displayName, @Nullable Uri photoUrl) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        preferences.edit().putString(PREF_USER_EMAIL, email)
                .putString(PREF_USER_NAME, displayName)
                .putString(PREF_USER_PHOTO, photoUrl != null ? photoUrl.toString() : "")
                .apply();
    }

    @Nullable
    public static String getUserEmail(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return preferences.getString(PREF_USER_EMAIL, null);
    }

    @NonNull
    public static String getUserName(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return preferences.getString(PREF_USER_NAME, "");
    }

    @NonNull
    public static String getUserPhoto(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return preferences.getString(PREF_USER_PHOTO, "");
    }

    public static void clearUser(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        preferences.edit().remove(PREF_USER_EMAIL).apply();
    }
}
