package com.teo.ttasks.ui.activities.task_detail

import com.teo.ttasks.data.local.WidgetHelper
import com.teo.ttasks.data.model.Task
import com.teo.ttasks.data.remote.TasksHelper
import com.teo.ttasks.jobs.DeleteTaskJob
import com.teo.ttasks.ui.base.Presenter
import com.teo.ttasks.util.NotificationHelper
import io.reactivex.disposables.Disposable
import io.realm.Realm
import timber.log.Timber

internal class TaskDetailPresenter(private val tasksHelper: TasksHelper,
                                   private val widgetHelper: WidgetHelper,
                                   private val notificationHelper: NotificationHelper) : Presenter<TaskDetailView>() {

    private lateinit var realm: Realm

    private lateinit var taskId: String

    private var taskSubscription: Disposable? = null

    /** Un-managed task */
    private var task: Task? = null

    internal fun getTask(taskId: String) {
        this.taskId = taskId
        taskSubscription?.let { if (!it.isDisposed) it.dispose() }
        taskSubscription = tasksHelper.getTaskAsSingle(taskId, realm)
                .subscribe(
                        { task ->
                            this.task = realm.copyFromRealm(task)
                            view()?.onTaskLoaded(this.task!!)
                        },
                        {
                            Timber.e(it, "Error while retrieving the task")
                            view()?.onTaskLoadError()
                        }
                )
        disposeOnUnbindView(taskSubscription!!)
    }

    internal fun getTaskList(taskListId: String) {
        val disposable = tasksHelper.getTaskListAsSingle(taskListId, realm)
                .subscribe(
                        { taskList ->
                            view()?.onTaskListLoaded(realm.copyFromRealm(taskList))
                        },
                        {
                            Timber.e(it, "Error while retrieving the task list")
                            view()?.onTaskListLoadError()
                        }
                )
        disposeOnUnbindView(disposable)
    }

    /**
     * Mark the task as completed if it isn't and vice versa.
     * If the task is completed, the completion date is set to the current date.
     */
    internal fun updateCompletionStatus() {
        task!!.let {
            tasksHelper.updateCompletionStatus(it, realm)
            realm.executeTransaction { _ -> realm.copyToRealmOrUpdate(it) }

            view()?.onTaskUpdated(it)

            // Trigger a widget update
            widgetHelper.updateWidgets(it.taskListId)

            // Reschedule the reminder if going from completed -> active
            if (!it.isCompleted) {
                notificationHelper.scheduleTaskNotification(it)
            }
        }
    }

    /**
     * Delete the task
     */
    internal fun deleteTask() {
        task?.let {
            if (it.isLocalOnly) {
                // Delete the task from the local database
                realm.executeTransaction { _ ->
                    // Make sure we're deleting a managed task
                    realm.copyToRealmOrUpdate(it).deleteFromRealm()
                }
            } else {
                // Mark it as deleted so it doesn't show up in the list
                realm.executeTransaction { _ ->
                    // Make sure we're marking a managed task as deleted
                    realm.copyToRealmOrUpdate(it).deleted = true
                }

                DeleteTaskJob.schedule(it.id, it.taskListId)
            }

            // Trigger a widget update only if the task is marked as active
            if (!it.isCompleted) {
                widgetHelper.updateWidgets(it.taskListId)
            }

            // Cancel the notification, if present
            notificationHelper.cancelTaskNotification(it.notificationId)

            view()?.onTaskDeleted()
        }
    }

    override fun bindView(view: TaskDetailView) {
        super.bindView(view)
        realm = Realm.getDefaultInstance()
    }

    override fun unbindView(view: TaskDetailView) {
        super.unbindView(view)
        realm.close()
    }
}
