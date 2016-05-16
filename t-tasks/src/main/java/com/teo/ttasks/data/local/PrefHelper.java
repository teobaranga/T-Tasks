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

    private SharedPreferences mSharedPreferences;

    public PrefHelper(Context context) {
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public boolean isUserPresent() {
        return mSharedPreferences.getString(PREF_USER_EMAIL, null) != null;
    }

    public void setUser(String email, String displayName, @Nullable Uri photoUrl) {
        mSharedPreferences.edit().putString(PREF_USER_EMAIL, email)
                .putString(PREF_USER_NAME, displayName)
                .putString(PREF_USER_PHOTO, photoUrl != null ? photoUrl.toString() : "")
                .apply();
    }

    @Nullable
    public String getUserEmail() {
        return mSharedPreferences.getString(PREF_USER_EMAIL, null);
    }

    @NonNull
    public String getUserName() {
        return mSharedPreferences.getString(PREF_USER_NAME, "");
    }

    @NonNull
    public String getUserPhoto() {
        return mSharedPreferences.getString(PREF_USER_PHOTO, "");
    }

    public void clearUser() {
        mSharedPreferences.edit().remove(PREF_USER_EMAIL).apply();
    }

    /**
     * Return the ID of the most recently accessed task list
     *
     * @return the task list ID
     */
    @Nullable
    public String getCurrentTaskListId() {
        return mSharedPreferences.getString(PREF_CURRENT_TASK_LIST_ID, null);
    }

    public void updateCurrentTaskList(String currentTaskListId) {
        mSharedPreferences.edit().putString(PREF_CURRENT_TASK_LIST_ID, currentTaskListId).apply();
    }
}
