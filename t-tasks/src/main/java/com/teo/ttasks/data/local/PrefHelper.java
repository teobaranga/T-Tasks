package com.teo.ttasks.data.local;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;

public final class PrefHelper {

    // NOTE: the shared preference item holding the etag saved for a given task list does not get deleted once the task list gets deleted

    private static final String PREF_USER_EMAIL = "email";
    private static final String PREF_USER_NAME = "name";
    private static final String PREF_USER_PHOTO = "photo";
    private static final String PREF_USER_COVER = "cover";

    private static final String PREF_CURRENT_TASK_LIST_ID = "currentTaskListId";
    private static final String PREF_ACCESS_TOKEN = "accessToken";

    private static final String PREF_TASK_LISTS_RESPONSE_ETAG = "taskListsEtag";

    private static final String PREF_WIDGET_PREFIX = "tasksWidget_";

    private SharedPreferences mSharedPreferences;

    public PrefHelper(Context context) {
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    /**
     * Check if a valid user is logged in
     *
     * @return {@code true} if an email and an access token are present, {@code false} otherwise
     */
    public boolean isUserPresent() {
        return mSharedPreferences.getString(PREF_USER_EMAIL, null) != null &&
                getAccessToken() != null;
    }

    public void setUser(String email, String displayName) {
        mSharedPreferences.edit().putString(PREF_USER_EMAIL, email)
                .putString(PREF_USER_NAME, displayName)
                .apply();
    }

    @Nullable
    public String getUserEmail() {
        return mSharedPreferences.getString(PREF_USER_EMAIL, null);
    }

    public String getUserName() {
        return mSharedPreferences.getString(PREF_USER_NAME, "");
    }

    @Nullable
    public String getUserPhoto() {
        return mSharedPreferences.getString(PREF_USER_PHOTO, null);
    }

    public void setUserPhoto(String photoUrl) {
        mSharedPreferences.edit().putString(PREF_USER_PHOTO, photoUrl).apply();
    }

    @Nullable
    public String getUserCover() {
        return mSharedPreferences.getString(PREF_USER_COVER, null);
    }

    public void setUserCover(String coverUrl) {
        mSharedPreferences.edit().putString(PREF_USER_COVER, coverUrl).apply();
    }

    public void clearUser() {
        mSharedPreferences.edit().remove(PREF_USER_EMAIL).apply();
    }

    @Nullable
    public String getAccessToken() {
        return mSharedPreferences.getString(PREF_ACCESS_TOKEN, null);
    }

    public void setAccessToken(String accessToken) {
        mSharedPreferences.edit().putString(PREF_ACCESS_TOKEN, accessToken).apply();
    }

    public void setTasksResponseEtag(String taskListId, String etag) {
        mSharedPreferences.edit().putString(taskListId, etag).apply();
    }

    public String getTasksResponseEtag(String taskListId) {
        return mSharedPreferences.getString(taskListId, "");
    }

    public String getTaskListsResponseEtag() {
        return mSharedPreferences.getString(PREF_TASK_LISTS_RESPONSE_ETAG, "");
    }

    public void setTaskListsResponseEtag(String etag) {
        mSharedPreferences.edit().putString(PREF_TASK_LISTS_RESPONSE_ETAG, etag).apply();
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

    /**
     * Save the task list ID associated with the specified widget
     *
     * @param appWidgetId tasks widget identifier
     * @param taskListId  task list identifier
     */
    public void setWidgetTaskListId(int appWidgetId, String taskListId) {
        mSharedPreferences.edit().putString(PREF_WIDGET_PREFIX + appWidgetId, taskListId).apply();
    }

    /**
     * Retrieve the task list ID associated with a given tasks widget
     *
     * @param appWidgetId task widget identifier
     * @return the task list identifier
     */
    @Nullable
    public String getWidgetTaskListId(int appWidgetId) {
        return mSharedPreferences.getString(PREF_WIDGET_PREFIX + appWidgetId, null);
    }

    public void deleteWidgetTaskId(int appWidgetId) {
        mSharedPreferences.edit().remove(PREF_WIDGET_PREFIX + appWidgetId).apply();
    }
}
