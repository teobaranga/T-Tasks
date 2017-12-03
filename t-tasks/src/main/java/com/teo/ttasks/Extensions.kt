package com.teo.ttasks

import com.teo.ttasks.data.model.TTask

/** Delete a task and all the Realm data associated with it. */
internal fun TTask.delete() {
    this.task.deleteFromRealm()
    this.deleteFromRealm()
}