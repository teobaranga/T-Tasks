package com.teo.ttasks.ui.fragments.tasks

import com.androidhuman.rxfirebase2.database.dataChangesOf
import com.androidhuman.rxfirebase2.database.model.DataValue
import com.google.android.gms.auth.UserRecoverableAuthException
import com.google.firebase.database.FirebaseDatabase
import com.teo.ttasks.data.local.PrefHelper
import com.teo.ttasks.data.model.TTask
import com.teo.ttasks.data.remote.TasksHelper
import com.teo.ttasks.ui.base.Presenter
import com.teo.ttasks.util.FirebaseUtil.getTasksDatabase
import com.teo.ttasks.util.FirebaseUtil.reminder
import com.teo.ttasks.util.RxUtils
import com.teo.ttasks.util.SortType
import com.teo.ttasks.util.TaskType
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.processors.PublishProcessor
import io.reactivex.schedulers.Schedulers
import io.realm.Realm
import timber.log.Timber
import java.util.*
import java.util.concurrent.TimeUnit

internal class TasksPresenter(private val tasksHelper: TasksHelper,
                              private val prefHelper: PrefHelper) : Presenter<TasksView>() {

    private var tasksSubscription: Disposable? = null

    private var sortingMode = prefHelper.sortMode

    private val reminderProcessor = PublishProcessor.create<TTask>()

    /**
     * Map of task IDs to their respective reminder disposable.
     * Used to keep the list of reminder updates synchronized with the tasks on add/delete.
     */
    private val reminderMap = hashMapOf<String, Disposable>()

    private val reminderDisposables = CompositeDisposable()

    private val tasks by lazy { FirebaseDatabase.getInstance().getTasksDatabase() }

    private lateinit var realm: Realm

    internal var showCompleted: Boolean
        get() = prefHelper.showCompleted
        set(showCompleted) {
            prefHelper.showCompleted = showCompleted
        }

    /**
     * Load and monitor changes to the tasks associated with the provided
     * task list from the local database.

     * @param taskListId task list identifier
     */
    internal fun subscribeToTasks(taskListId: String, resubscribe: Boolean = false) {
        var taskCount = 0

        // Since Realm observables do not complete, this subscription must be recreated every time
        tasksSubscription?.let { if (!it.isDisposed) it.dispose() }
        if (!resubscribe) {
            view()?.onTasksLoading()
        }

        tasksSubscription = tasksHelper.getTasks(taskListId, realm)
                .map { realm.copyFromRealm<TTask>(it) }
                .observeOn(Schedulers.io())
                .doOnNext { tTasks ->
                    val tTasksMap = tTasks.associateBy({ it.id }, { it })

                    // Dispose reminders for deleted tasks
                    val deletedTaskIds = reminderMap.keys - tTasksMap.keys
                    deletedTaskIds.forEach { id ->
                        // Remove and dispose
                        reminderDisposables.remove(reminderMap.getValue(id))
                        reminderMap.remove(id)
//                        Timber.v("Removed listener for task $id")
                    }

                    // Add reminder listeners for new tasks
                    val newTaskIds = tTasksMap.keys - reminderMap.keys
                    newTaskIds.forEach { id ->
                        val reminderDisposable = tasks.reminder(id)!!.dataChangesOf<Long>()
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribeOn(Schedulers.io())
                                .subscribe(
                                        { t ->
                                            if (t == DataValue.empty<Long>()) {
//                                                Timber.v("Reminder for $id: empty")
                                            } else {
                                                val dateInMillis = t.value()
//                                                Timber.v("Reminder for $id: $dateInMillis")
                                                val reminder = Date(dateInMillis)
                                                val tTask = tTasksMap[id]!!
                                                if (tTask.reminder != reminder) {
                                                    tTask.reminder = reminder
                                                    reminderProcessor.onNext(tTask)
                                                }
                                            }
                                        }
                                )
                        reminderDisposables.add(reminderDisposable)
                        reminderMap[id] = reminderDisposable
//                        Timber.v("Added listener for task $id")
                    }
                }
                .compose(RxUtils.getTaskItems(sortingMode))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        { (taskType, taskItems) ->
                            taskCount += taskItems.size
                            when (taskType) {
                                TaskType.ACTIVE -> {
                                    // Show active tasks
                                    view()?.onActiveTasksLoaded(taskItems)
                                }
                                TaskType.COMPLETED -> {
                                    // Show completed tasks
                                    view()?.onCompletedTasksLoaded(taskItems)
                                }
                                // End of tasks - all the previous task types have been processed
                                TaskType.EOT -> {
                                    view()?.let {
                                        if (taskCount == 0) {
                                            it.onTasksEmpty()
                                        } else {
                                            it.onTasksLoaded()
                                            taskCount = 0
                                        }
                                    }
                                }
                            }
                        },
                        { Timber.e(it, "Error when subscribing to tasks")}
                )
        disposeOnUnbindView(tasksSubscription!!)
    }

    internal fun refreshTasks(taskListId: String?) {
        if (taskListId == null)
            return
        val subscription = tasksHelper.refreshTasks(taskListId)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        { view()?.onRefreshDone() },
                        { throwable ->
                            Timber.e(throwable, "Error refreshing tasks")
                            view()?.let {
                                val cause = throwable.cause
                                when (cause) {
                                    is UserRecoverableAuthException -> it.onTasksLoadError(cause.intent)
                                    else -> it.onTasksLoadError(null)
                                }
                            }
                        })
        disposeOnUnbindView(subscription)
    }

    /**
     * Synchronize the local tasks from the specified task list.
     * Tasks that have only been updated locally are uploaded to the Google servers.
     *
     * @param taskListId task list identifier
     */
    internal fun syncTasks(taskListId: String?) {
        if (taskListId == null)
            return
        // Keep track of the number of synced tasks
        var taskSyncCount = 0
        val subscription = tasksHelper.syncTasks(taskListId)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        { syncedTask ->
                            // Sync successful for this task
                            realm.executeTransaction { realm ->
                                syncedTask.synced = true
                                // This task is not managed by Realm so it needs to be updated manually
                                realm.insertOrUpdate(syncedTask)
                            }
                            taskSyncCount++
                        },
                        {
                            // Sync failed for at least one task, will retry on next refresh
                            Timber.e(it, "Error synchronizing tasks")
                            view()?.onSyncDone(taskSyncCount)
                        },
                        {
                            // Syncing done
                            view()?.onSyncDone(taskSyncCount)
                        }
                )
        disposeOnUnbindView(subscription)
    }

    /**
     * Switch the sorting mode.

     * @param sortingMode the new sorting mode
     * *
     * @return true if the new sorting mode is different, false otherwise
     */
    internal fun switchSortMode(sortingMode: SortType): Boolean {
        if (sortingMode != this.sortingMode) {
            this.sortingMode = sortingMode
            prefHelper.sortMode = sortingMode
            return true
        }
        return false
    }

    override fun bindView(view: TasksView) {
        super.bindView(view)
        realm = Realm.getDefaultInstance()
        // TODO: optimize this since it still runs in the background...
        reminderProcessor
                .buffer(1, TimeUnit.SECONDS)
                .filter { tTasks -> tTasks.size != 0 }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        { tTasks ->
                            realm.executeTransaction { it.copyToRealmOrUpdate(tTasks) }
                            Timber.v("Processed reminders for %d tasks", tTasks.size)
                        },
                        { Timber.e(it, "Error while processing the reminders") }
                )
    }

    override fun unbindView(view: TasksView) {
        super.unbindView(view)
        reminderDisposables.clear()
        reminderMap.clear()
        realm.close()
    }
}
