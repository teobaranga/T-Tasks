package com.teo.ttasks.data.local

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import com.teo.ttasks.util.NightHelper.NIGHT_AUTO
import com.teo.ttasks.util.SortType

class PrefHelper(context: Context) {

    companion object {

        // FIXME: the shared preference item holding the etag saved for a given task list does not get deleted once the task list gets deleted

        private const val PREF_USER_EMAIL = "email"
        private const val PREF_USER_NAME = "name"
        private const val PREF_USER_PHOTO = "photo"
        private const val PREF_USER_COVER = "cover"

        private const val PREF_CURRENT_TASK_LIST_ID = "currentTaskListId"
        private const val PREF_ACCESS_TOKEN = "accessToken"

        private const val PREF_TASK_LISTS_RESPONSE_ETAG = "taskListsEtag"
        private const val PREF_TASKS_RESPONSE_ETAG_PREFIX = "etag_"

        private const val PREF_WIDGET_PREFIX = "tasksWidget_"

        private const val PREF_SHOW_COMPLETED = "showCompleted"

        private const val PREF_SORT_MODE = "sortMode"

        private const val PREF_NIGHT_MODE = "night_mode"
    }

    private val sharedPreferences: SharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(context)
    }

    val nightMode: String
        get() = sharedPreferences.getString(PREF_NIGHT_MODE, NIGHT_AUTO)!!

    val userEmail: String?
        get() = sharedPreferences.getString(PREF_USER_EMAIL, null)

    val userName: String
        get() = sharedPreferences.getString(PREF_USER_NAME, "")!!

    var userPhoto: String?
        get() = sharedPreferences.getString(PREF_USER_PHOTO, null)
        set(photoUrl) = sharedPreferences.edit().putString(PREF_USER_PHOTO, photoUrl).apply()

    var userCover: String?
        get() = sharedPreferences.getString(PREF_USER_COVER, null)
        set(coverUrl) = sharedPreferences.edit().putString(PREF_USER_COVER, coverUrl).apply()

    var accessToken: String?
        get() = sharedPreferences.getString(PREF_ACCESS_TOKEN, null)
        set(accessToken) = sharedPreferences.edit().putString(PREF_ACCESS_TOKEN, accessToken).apply()

    var taskListsResponseEtag: String
        get() = sharedPreferences.getString(PREF_TASK_LISTS_RESPONSE_ETAG, "")!!
        set(etag) = sharedPreferences.edit().putString(PREF_TASK_LISTS_RESPONSE_ETAG, etag).apply()

    /** Return the ID of the most recently accessed task list */
    var currentTaskListId: String?
        get() = sharedPreferences.getString(PREF_CURRENT_TASK_LIST_ID, null)
        set(currentTaskListId) = sharedPreferences.edit().putString(PREF_CURRENT_TASK_LIST_ID, currentTaskListId).apply()

    /** Flag indicating whether to show completed tasks as expanded at startup */
    var showCompleted: Boolean
        get() = sharedPreferences.getBoolean(PREF_SHOW_COMPLETED, true)
        set(showCompleted) = sharedPreferences.edit().putBoolean(PREF_SHOW_COMPLETED, showCompleted).apply()

    var sortMode: SortType
        get() = SortType.valueOf(sharedPreferences.getString(PREF_SORT_MODE, null) ?: SortType.SORT_DATE.name)
        set(sortMode) = sharedPreferences.edit().putString(PREF_SORT_MODE, sortMode.name).apply()

    fun setUser(email: String, displayName: String) {
        sharedPreferences.edit().putString(PREF_USER_EMAIL, email)
                .putString(PREF_USER_NAME, displayName)
                .apply()
    }

    fun clearUser() {
        sharedPreferences.edit().clear().apply()
    }

    fun setTasksResponseEtag(taskListId: String, etag: String) {
        sharedPreferences.edit().putString(PREF_TASKS_RESPONSE_ETAG_PREFIX + taskListId, etag).apply()
    }

    fun getTasksResponseEtag(taskListId: String): String {
        return sharedPreferences.getString(PREF_TASKS_RESPONSE_ETAG_PREFIX + taskListId, "")!!
    }

    fun deleteAllEtags() {
        sharedPreferences.all.keys
                .filter { it.startsWith(PREF_TASKS_RESPONSE_ETAG_PREFIX) }
                .forEach { key -> sharedPreferences.edit().remove(key).apply() }

        sharedPreferences.edit().remove(PREF_TASK_LISTS_RESPONSE_ETAG).apply()
    }

    /**
     * Save the task list ID associated with the specified widget
     * @param appWidgetId tasks widget identifier
     * @param taskListId  task list identifier
     */
    fun setWidgetTaskListId(appWidgetId: Int, taskListId: String) {
        sharedPreferences.edit().putString(PREF_WIDGET_PREFIX + appWidgetId, taskListId).apply()
    }

    /**
     * Retrieve the task list ID associated with a given tasks widget
     * @param appWidgetId task widget identifier
     * @return the task list identifier
     */
    fun getWidgetTaskListId(appWidgetId: Int): String? {
        return sharedPreferences.getString(PREF_WIDGET_PREFIX + appWidgetId, null)
    }

    fun deleteWidgetTaskId(appWidgetId: Int) {
        sharedPreferences.edit().remove(PREF_WIDGET_PREFIX + appWidgetId).apply()
    }
}
