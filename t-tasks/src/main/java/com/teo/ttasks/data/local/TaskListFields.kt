package com.teo.ttasks.data.local

import java.util.*

class TaskListFields : HashMap<String, Any>() {

    companion object {
        private const val KEY_TITLE = "title"
    }

    var title: String?
        get() = get(KEY_TITLE) as? String
        set(title) = title.let { if (title == null || title.isBlank()) remove(KEY_TITLE) else put(KEY_TITLE, title) }
}
