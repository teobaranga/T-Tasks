package com.teo.ttasks.data.local;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class PrefHelper {

    // NOTE: the shared preference item holding the etag saved for a given task list does not get deleted once the task list gets deleted

    private static final String PREF_USER_EMAIL = "email";
    private static final String PREF_USER_NAME = "name";
    private static final String PREF_USER_PHOTO = "photo";
    private static final String PREF_USER_COVER = "cover";

    private static final String PREF_CURRENT_TASK_LIST_ID = "currentTaskListId";
    private static final String PREF_ACCESS_TOKEN = "accessToken";

    private static final String PREF_TASK_LISTS_RESPONSE_ETAG = "taskListsEtag";
    private static final String PREF_TASKS_RESPONSE_ETAG_PREFIX = "etag_";

    private static final String PREF_WIDGET_PREFIX = "tasksWidget_";

    private static final String PREF_LAST_TASK_ID = "lastTaskId";

    private static final String PREF_HIDE_COMPLETED = "hideCompleted";

    private SharedPreferences sharedPreferences;

    public PrefHelper(Context context) {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    /**
     * Check if a valid user is logged in
     *
     * @return {@code true} if an email and an access token are present, {@code false} otherwise
     */
    public boolean isUserPresent() {
        return sharedPreferences.getString(PREF_USER_EMAIL, null) != null &&
                getAccessToken() != null;
    }

    public void setUser(String email, String displayName) {
        sharedPreferences.edit().putString(PREF_USER_EMAIL, email)
                .putString(PREF_USER_NAME, displayName)
                .apply();
    }

    @Nullable
    public String getUserEmail() {
        return sharedPreferences.getString(PREF_USER_EMAIL, null);
    }

    public String getUserName() {
        return sharedPreferences.getString(PREF_USER_NAME, "");
    }

    @Nullable
    public String getUserPhoto() {
        return sharedPreferences.getString(PREF_USER_PHOTO, null);
    }

    public void setUserPhoto(String photoUrl) {
        sharedPreferences.edit().putString(PREF_USER_PHOTO, photoUrl).apply();
    }

    @Nullable
    public String getUserCover() {
        return sharedPreferences.getString(PREF_USER_COVER, null);
    }

    public void setUserCover(String coverUrl) {
        sharedPreferences.edit().putString(PREF_USER_COVER, coverUrl).apply();
    }

    public void clearUser() {
        sharedPreferences.edit().remove(PREF_USER_EMAIL).apply();
    }

    @Nullable
    public String getAccessToken() {
        return sharedPreferences.getString(PREF_ACCESS_TOKEN, null);
    }

    public void setAccessToken(String accessToken) {
        sharedPreferences.edit().putString(PREF_ACCESS_TOKEN, accessToken).apply();
    }

    public void setTasksResponseEtag(String taskListId, String etag) {
        sharedPreferences.edit().putString(PREF_TASKS_RESPONSE_ETAG_PREFIX + taskListId, etag).apply();
    }

    public String getTasksResponseEtag(String taskListId) {
        return sharedPreferences.getString(PREF_TASKS_RESPONSE_ETAG_PREFIX + taskListId, "");
    }

    public String getTaskListsResponseEtag() {
        return sharedPreferences.getString(PREF_TASK_LISTS_RESPONSE_ETAG, "");
    }

    public void setTaskListsResponseEtag(String etag) {
        sharedPreferences.edit().putString(PREF_TASK_LISTS_RESPONSE_ETAG, etag).apply();
    }

    public void deleteAllEtags() {
        List<String> etagsToDelete = new ArrayList<>();

        for (String key : sharedPreferences.getAll().keySet())
            if (key.startsWith(PREF_TASKS_RESPONSE_ETAG_PREFIX))
                etagsToDelete.add(key);

        for (String key : etagsToDelete)
            sharedPreferences.edit().remove(key).apply();

        sharedPreferences.edit().remove(PREF_TASK_LISTS_RESPONSE_ETAG).apply();
    }

    /**
     * Return the ID of the most recently accessed task list
     *
     * @return the task list ID
     */
    @Nullable
    public String getCurrentTaskListId() {
        return sharedPreferences.getString(PREF_CURRENT_TASK_LIST_ID, null);
    }

    public void setLastAccessedTaskList(String currentTaskListId) {
        sharedPreferences.edit().putString(PREF_CURRENT_TASK_LIST_ID, currentTaskListId).apply();
    }

    /**
     * Save the task list ID associated with the specified widget
     *
     * @param appWidgetId tasks widget identifier
     * @param taskListId  task list identifier
     */
    public void setWidgetTaskListId(int appWidgetId, String taskListId) {
        sharedPreferences.edit().putString(PREF_WIDGET_PREFIX + appWidgetId, taskListId).apply();
    }

    /**
     * Retrieve the task list ID associated with a given tasks widget
     *
     * @param appWidgetId task widget identifier
     * @return the task list identifier
     */
    @Nullable
    public String getWidgetTaskListId(int appWidgetId) {
        return sharedPreferences.getString(PREF_WIDGET_PREFIX + appWidgetId, null);
    }

    public void deleteWidgetTaskId(int appWidgetId) {
        sharedPreferences.edit().remove(PREF_WIDGET_PREFIX + appWidgetId).apply();
    }

    /**
     * Get a task ID to be used when creating a new task locally.
     *
     * @return a task list identifier
     */
    public String getNextTaskId() {
        String nextTaskId = sharedPreferences.getString(PREF_LAST_TASK_ID, "1");
        // Increment the ID and save it
        sharedPreferences.edit().putString(PREF_LAST_TASK_ID, String.valueOf(Integer.valueOf(nextTaskId) + 1)).apply();
        return nextTaskId;
    }

    /**
     * Must be called after deleting a local-only task or after a local task is synced.
     */
    public void deleteLastTaskId() {
        String lastTaskId = sharedPreferences.getString(PREF_LAST_TASK_ID, null);
        if (lastTaskId == null)
            return;
        // Remove or decrease the last task ID
        final SharedPreferences.Editor edit = sharedPreferences.edit();
        final Integer id = Integer.valueOf(lastTaskId);
        if (id <= 1) {
            edit.remove(PREF_LAST_TASK_ID);
        } else {
            edit.putString(PREF_LAST_TASK_ID, String.valueOf(id - 1));
        }
        edit.apply();
    }

    public void setHideCompleted(boolean hideCompleted) {
        sharedPreferences.edit().putBoolean(PREF_HIDE_COMPLETED, hideCompleted).apply();
    }

    public boolean getHideCompleted() {
        return sharedPreferences.getBoolean(PREF_HIDE_COMPLETED, false);
    }
}
