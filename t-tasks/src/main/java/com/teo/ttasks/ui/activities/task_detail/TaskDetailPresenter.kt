package com.teo.ttasks.ui.activities.task_detail

import com.birbit.android.jobqueue.Job
import com.birbit.android.jobqueue.JobManager
import com.birbit.android.jobqueue.callback.JobManagerCallback
import com.birbit.android.jobqueue.callback.JobManagerCallbackAdapter
import com.teo.ttasks.data.local.WidgetHelper
import com.teo.ttasks.data.model.TTask
import com.teo.ttasks.data.remote.TasksHelper
import com.teo.ttasks.delete
import com.teo.ttasks.jobs.CreateTaskJob
import com.teo.ttasks.jobs.DeleteTaskJob
import com.teo.ttasks.ui.base.Presenter
import com.teo.ttasks.util.NotificationHelper
import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.realm.Realm
import timber.log.Timber

internal class TaskDetailPresenter(private val tasksHelper: TasksHelper, private val widgetHelper: WidgetHelper,
                                   private val notificationHelper: NotificationHelper, private val jobManager: JobManager) : Presenter<TaskDetailView>() {

    private lateinit var realm: Realm

    private lateinit var taskId: String

    private var jobManagerCallback: JobManagerCallback? = null

    private var taskSubscription: Disposable? = null

    private var tTask: TTask? = null

    internal fun getTask(taskId: String) {
        this.taskId = taskId
        taskSubscription?.let { if (!it.isDisposed) it.dispose() }
        taskSubscription = tasksHelper.getTaskAsSingle(taskId, realm)
                .subscribe(
                        { tTask ->
                            this.tTask = realm.copyFromRealm(tTask)
                            view()?.onTaskLoaded(this.tTask!!)
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
        tasksHelper.updateCompletionStatus(tTask!!, realm)
                .subscribe({ }, { throwable ->
                    // Update unsuccessful, keep the task marked as "not synced"
                    // The app will retry later, as soon as the user is online
                    Timber.e(throwable.toString())
                })

        tTask?.let {
            view()?.onTaskUpdated(it)

            if (!it.isCompleted) {
                // Trigger a widget update only if the task is marked as active
                widgetHelper.updateWidgets(it.taskListId)
                notificationHelper.scheduleTaskNotification(it)
            }
        }
    }

    /**
     * Delete the task
     */
    internal fun deleteTask() {
        tTask?.let {
            if (it.isLocalOnly) {
                // Delete the task from the local database
                realm.executeTransaction { _ -> it.delete() }

            } else {
                // Mark it as deleted so it doesn't show up in the list
                realm.executeTransaction { _ -> it.deleted = true }

                jobManager.addJobInBackground(DeleteTaskJob(it.id, it.taskListId))
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
        jobManagerCallback = object : JobManagerCallbackAdapter() {
            override fun onJobRun(job: Job, resultCode: Int) {
                Flowable.defer {
                    if (job is CreateTaskJob) {
                        if (job.localTaskId == taskId && resultCode == JobManagerCallback.RESULT_SUCCEED) {
                            // Update the task
                            getTask(job.onlineTaskId)
                        }
                    }
                    Flowable.empty<Any>()
                }.subscribeOn(AndroidSchedulers.mainThread()).subscribe()
            }
        }
        jobManager.addCallback(jobManagerCallback)
    }

    override fun unbindView(view: TaskDetailView) {
        super.unbindView(view)
        realm.close()
        jobManager.removeCallback(jobManagerCallback)
        jobManagerCallback = null
    }
}
