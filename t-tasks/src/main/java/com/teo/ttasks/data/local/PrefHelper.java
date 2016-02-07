package com.teo.ttasks.data.local;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;

public class PrefHelper {

    private static final String PREF_USER_EMAIL = "email";
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

    public static void setUser(Context context, String email) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        preferences.edit().putString(PREF_USER_EMAIL, email).apply();
    }

    public static void clearUser(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        preferences.edit().remove(PREF_USER_EMAIL).apply();
    }
}
