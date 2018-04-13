package com.teo.ttasks.data.local

import com.teo.ttasks.data.model.TTaskList
import com.teo.ttasks.data.model.TaskList
import java.util.*

class TaskListFields : HashMap<String, Any>() {

    companion object {
        private const val KEY_TITLE = "title"
    }

    var title: String?
        get() = get(KEY_TITLE) as? String
        set(title) = title.let { if (title == null || title.isBlank()) remove(KEY_TITLE) else put(KEY_TITLE, title) }

    /**
     * Create a task list locally and mark it as not synced.
     * This task list needs to be copied to Realm in order to make it persistent.
     *
     * @return a local, un-managed task list
     */
    fun toTaskList(): TTaskList {
        // Create the task list
        val taskList = TaskList()
        taskList.title = this.title!!

        // Create the TTaskList
        val tTaskList = TTaskList(taskList)
        tTaskList.synced = false

        return tTaskList
    }
}
