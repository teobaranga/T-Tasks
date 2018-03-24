package com.teo.ttasks.ui.fragments.tasks

import com.androidhuman.rxfirebase2.database.dataChangesOf
import com.androidhuman.rxfirebase2.database.model.DataValue
import com.google.android.gms.auth.UserRecoverableAuthException
import com.google.firebase.database.FirebaseDatabase
import com.teo.ttasks.data.local.PrefHelper
import com.teo.ttasks.data.model.TTask
import com.teo.ttasks.data.remote.TasksHelper
import com.teo.ttasks.ui.base.Presenter
import com.teo.ttasks.ui.items.TaskItem
import com.teo.ttasks.util.FirebaseUtil.getTasksDatabase
import com.teo.ttasks.util.FirebaseUtil.reminder
import com.teo.ttasks.util.RxUtils
import com.teo.ttasks.util.SortType
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.flowables.GroupedFlowable
import io.reactivex.processors.PublishProcessor
import io.reactivex.schedulers.Schedulers
import io.realm.Realm
import timber.log.Timber
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

internal class TasksPresenter(private val tasksHelper: TasksHelper,
                              private val prefHelper: PrefHelper) : Presenter<TasksView>() {

    private var tasksSubscription: Disposable? = null

    private var sortingMode = prefHelper.sortMode

    internal var showCompleted: Boolean
        get() = prefHelper.showCompleted
        set(showCompleted) {
            prefHelper.showCompleted = showCompleted
        }

    private lateinit var realm: Realm

    private val reminderProcessor = PublishProcessor.create<TTask>()

    /**
     * Map of task IDs to their respective reminder disposable.
     * Used to keep the list of reminder updates synchronized with the tasks on add/delete.
     */
    private val reminderMap = hashMapOf<String, Disposable>()

    private val reminderDisposables = CompositeDisposable()

    private val tasks by lazy { FirebaseDatabase.getInstance().getTasksDatabase() }

    /**
     * Load and monitor changes to the tasks associated with the provided
     * task list from the local database.

     * @param taskListId task list identifier
     */
    internal fun subscribeToTasks(taskListId: String) {
        var taskCount = 0

        // Since Realm observables do not complete, this subscription must be recreated every time
        tasksSubscription?.let { if (!it.isDisposed) it.dispose() }
        view()?.onTasksLoading()

        tasksSubscription = tasksHelper.getTasks(taskListId, realm)
                .doOnNext { tTasks ->
                    val tTasksMap = tTasks.associateBy({ it.id }, { it })

                    // Dispose reminders for deleted tasks
                    val deletedTaskIds = reminderMap.keys - tTasksMap.keys
                    deletedTaskIds.forEach { id ->
                        // Remove and dispose
                        reminderDisposables.remove(reminderMap.getValue(id))
                        reminderMap.remove(id)
                        Timber.v("Removed listener for task $id")
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
                                                Timber.v("Reminder for $id: empty")
                                            } else {
                                                val dateInMillis = t.value()
                                                Timber.v("Reminder for $id: $dateInMillis")
                                                val reminder = Date(dateInMillis)
                                                if (tTasksMap[id]!!.reminder != reminder) {
                                                    val tTask = realm.copyFromRealm(tTasksMap[id]!!)
                                                    tTask.reminder = reminder
                                                    reminderProcessor.onNext(tTask)
                                                }
                                            }
                                        }
                                )
                        reminderDisposables.add(reminderDisposable)
                        reminderMap[id] = reminderDisposable
                        Timber.v("Added listener for task $id")
                    }
                }
                .compose<GroupedFlowable<Boolean, List<TaskItem>>>(RxUtils.getTaskItems(sortingMode))
                .flatMap { groupedFlowable ->
                    groupedFlowable.doOnNext { taskItems ->
                        if (groupedFlowable.key!!) {
                            view()?.let {
                                it.onActiveTasksLoaded(taskItems)
                                if (!taskItems.isEmpty()) taskCount += taskItems.size
                            }
                        } else {
                            view()?.let {
                                // Show completed tasks
                                it.onCompletedTasksLoaded(taskItems)
                                if (!taskItems.isEmpty()) taskCount += taskItems.size

                                if (taskCount == 0) {
                                    it.onTasksEmpty()
                                } else {
                                    it.onTasksLoaded()
                                    taskCount = 0
                                }
                            }
                        }
                    }
                }
                .subscribe()
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
                            Timber.e(throwable.toString())
                            val view = view()
                            if (view != null) {
                                if (throwable.cause is UserRecoverableAuthException)
                                    view.onTasksLoadError((throwable.cause as UserRecoverableAuthException).intent)
                                else
                                    view.onTasksLoadError(null)
                            }
                        })
        disposeOnUnbindView(subscription)
    }

    /**
     * Synchronize the local tasks from the specified task list.

     * @param taskListId task list identifier
     */
    internal fun syncTasks(taskListId: String?) {
        if (taskListId == null)
            return
        // Keep track of the number of synced tasks
        val taskSyncCount = AtomicInteger(0)
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
                            taskSyncCount.incrementAndGet()
                        },
                        { throwable ->
                            // Sync failed for at least one task, will retry on next refresh
                            Timber.e(throwable.toString())
                            val view = view()
                            view?.onSyncDone(taskSyncCount.get())
                        }
                ) {
                    // Syncing done
                    val view = view()
                    view?.onSyncDone(taskSyncCount.get())
                }
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
