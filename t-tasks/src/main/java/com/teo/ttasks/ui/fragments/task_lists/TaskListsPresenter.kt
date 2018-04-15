package com.teo.ttasks.ui.fragments.task_lists

import com.teo.ttasks.data.local.TaskListFields
import com.teo.ttasks.data.remote.TasksHelper
import com.teo.ttasks.ui.base.Presenter
import com.teo.ttasks.ui.items.TaskListItem
import io.reactivex.android.schedulers.AndroidSchedulers
import io.realm.Realm
import timber.log.Timber
import java.util.*

internal class TaskListsPresenter(private val tasksHelper: TasksHelper) : Presenter<TaskListsView>() {

    private val taskListFields: TaskListFields = TaskListFields()

    private lateinit var realm: Realm

    /**
     * Load all the user's task lists.
     */
    internal fun getTaskLists() {
        view()?.onTaskListsLoading()

        val disposable = tasksHelper.getTaskLists(realm)
                .map { taskLists ->
                    val taskListItems = ArrayList<TaskListItem>(taskLists.size)

                    taskLists.forEach {
                        taskListItems.add(TaskListItem(it, tasksHelper.getTaskListSize(it.id, realm)))
                    }

                    taskListItems
                }
                .subscribe { taskListItems ->
                    when {
                        taskListItems == null -> view()?.onTaskListsError()
                        taskListItems.isEmpty() -> view()?.onTaskListsEmpty()
                        else -> view()?.onTaskListsLoaded(taskListItems)
                    }
                }
        disposeOnUnbindView(disposable)
    }

    /**
     * Set the task list title.

     * @param title the new task list title
     */
    internal fun setTaskListTitle(title: String) {
        taskListFields.title = title
    }

    internal fun createTaskList() {
        // Nothing entered
        if (taskListFields.isEmpty())
            return

        // Create the task list offline
        val localTaskList = taskListFields.toTaskList()
        Timber.d("New task list with id %s", localTaskList.id)
        realm.executeTransaction { it.insertOrUpdate(localTaskList) }

        // Create the task remotely
        // TODO schedule job for this
        tasksHelper.createTaskList(taskListFields)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        { taskList ->
                            // Delete the old task list and insert the new one
                            val managedTaskList = tasksHelper.getTaskList(localTaskList.id, realm)!!
                            realm.executeTransaction {
                                managedTaskList.deleteFromRealm()
                                taskList.synced = true
                                it.insertOrUpdate(taskList)
                            }
                            Timber.d("Task list id updated to %s", managedTaskList.id)
                        },
                        { Timber.e(it, "Error while creating a new task list") }
                )
    }

    /**
     * Update the task list with the specified ID. Currently, only a title change is supported.

     * @param taskListId task list identifier
     * *
     * @param isOnline   flag indicating an active network connection
     */
    internal fun updateTaskList(taskListId: String, isOnline: Boolean) {
        // Nothing changed
        if (taskListFields.isEmpty())
            return

        // Update locally
        val managedTaskList = tasksHelper.getTaskList(taskListId, realm)!!
        realm.executeTransaction {
            managedTaskList.update(taskListFields)
            managedTaskList.synced = false
        }

        // Update remotely
        if (isOnline) {
            tasksHelper.updateTaskList(taskListId, taskListFields)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            { taskList ->
                                realm.executeTransaction {
                                    it.insertOrUpdate(taskList)
                                    managedTaskList.synced = true
                                }
                            },
                            {
                                Timber.e(it, "Error while updating task list %s", taskListId)
                                view()?.onTaskListUpdateError()
                            }
                    )
        }
    }

    /**
     * Delete the task list with the specified ID.

     * @param taskListId task list identifier
     */
    internal fun deleteTaskList(taskListId: String) {
        // Get the task list
        val managedTaskList = tasksHelper.getTaskList(taskListId, realm)!!

        // Mark it as deleted so it doesn't show up in the list
        realm.executeTransaction { managedTaskList.deleted = true }

        // Delete the task list
        tasksHelper.deleteTaskList(taskListId)
                .subscribe({ /* Do nothing */ }, { throwable -> Timber.e(throwable.toString()) })
    }

    override fun bindView(view: TaskListsView) {
        super.bindView(view)
        realm = Realm.getDefaultInstance()
    }

    override fun unbindView(view: TaskListsView) {
        super.unbindView(view)
        realm.close()
    }
}
