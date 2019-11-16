package com.teo.ttasks.util

enum class TaskType(val id: Int) {
    ACTIVE(1),
    COMPLETED(2),
    EOT(-1) // End of tasks, a little trick to indicate the end of the tasks stream
}
