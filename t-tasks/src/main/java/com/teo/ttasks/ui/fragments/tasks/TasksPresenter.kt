package com.teo.ttasks.ui.fragments.tasks

import com.androidhuman.rxfirebase2.database.data
import com.androidhuman.rxfirebase2.database.dataChanges
import com.google.android.gms.auth.UserRecoverableAuthException
import com.google.firebase.database.FirebaseDatabase
import com.teo.ttasks.data.local.PrefHelper
import com.teo.ttasks.data.model.TTask
import com.teo.ttasks.data.remote.TasksHelper
import com.teo.ttasks.ui.base.Presenter
import com.teo.ttasks.ui.items.TaskItem
import com.teo.ttasks.util.RxUtils
import com.teo.ttasks.util.SortType
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.flowables.GroupedFlowable
import io.reactivex.schedulers.Schedulers
import io.realm.Realm
import timber.log.Timber
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

internal class TasksPresenter(private val tasksHelper: TasksHelper, private val prefHelper: PrefHelper) : Presenter<TasksView>() {

    private var tasksSubscription: Disposable? = null

    private var sortingMode : SortType = SortType.SORT_DATE

    internal lateinit var realm: Realm

    init {
        sortingMode = prefHelper.sortMode
    }

    /**
     * Load the tasks associated with the provided task list from the local database.

     * @param taskListId task list identifier
     */
    internal fun getTasks(taskListId: String) {
        var taskCount = 0
        // Since Realm observables do not complete, this subscription must be recreated every time
        tasksSubscription?.let { if (!it.isDisposed) it.dispose() }
        run {
            val view = view()
            view?.onTasksLoading()
        }
        val tasks = FirebaseDatabase.getInstance().getReference("tasks")
        val disposables = CompositeDisposable()
        var initialized = false
        tasksSubscription = tasksHelper.getTasks(taskListId, realm)
                .doOnNext { tTasks ->
                    if (!initialized) {
                        initialized = true

                        val map = hashMapOf<TTask, Date>()

                        Observable.fromIterable(tTasks)
                                .flatMapSingle { tTask ->
                                    Timber.d("Getting reminders for ${tTask.id} on ${Thread.currentThread()}")
                                    tasks.child(tTask.id).child("reminder").data()
                                            .subscribeOn(Schedulers.io())
                                            .observeOn(AndroidSchedulers.mainThread())
                                            .doOnSuccess { dataSnapshot ->
                                                Timber.d("Got reminder for ${tTask.id} on ${Thread.currentThread()}: $dataSnapshot")
                                                if (dataSnapshot.exists()) {
                                                    val reminder = Date(dataSnapshot.getValue(Long::class.java)!!)
                                                    if (tTask.reminder != reminder) {
                                                        map[tTask] = reminder
                                                    }
                                                }
                                            }
                                }
                                .subscribe({}, {}, {
                                    Timber.d("completed")
                                    if (map.isNotEmpty()) {
                                        realm.executeTransaction {
                                            for ((tTask, reminder) in map) {
                                                tTask.reminder = reminder
                                                Timber.d("Restored reminder for %s", tTask.id)
                                            }
                                        }
                                    }
                                    Timber.d("Subscribing...")

                                    for (tTask in tTasks) {
                                        // Add a listener in order to get notified whenever the reminder date changes
                                        // TODO: 2016-10-01 remove this at sign out
                                        disposables.add(tasks.child(tTask.id).child("reminder").dataChanges().subscribe { newDataSnapshot ->
                                            if (newDataSnapshot.exists()) {
                                                val reminder = Date(newDataSnapshot.getValue(Long::class.java)!!)
                                                if (tTask.reminder != reminder) {
                                                    realm.executeTransaction { tTask.reminder = reminder }
                                                    Timber.d("New reminder retrieved for %s", tTask.id)
                                                }
                                            }
                                        })
                                    }
                                })
                    }
                }
                .compose<GroupedFlowable<Boolean, List<TaskItem>>>(RxUtils.getTaskItems(sortingMode))
                .flatMap { groupedFlowable ->
                    groupedFlowable.doOnNext { taskItems ->
                        if (groupedFlowable.key!!) {
                            Timber.d("loaded %d active tasks", taskItems.size)
                            view()?.let {
                                it.onActiveTasksLoaded(taskItems)
                                if (!taskItems.isEmpty()) taskCount += taskItems.size
                            }
                        } else {
                            Timber.d("loaded %d completed tasks", taskItems.size)
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
                        { /* ignored since onCompleted does the job, even when the tasks have not been updated */ },
                        { throwable ->
                            Timber.e(throwable.toString())
                            val view = view()
                            if (view != null) {
                                if (throwable.cause is UserRecoverableAuthException)
                                    view.onTasksLoadError((throwable.cause as UserRecoverableAuthException).intent)
                                else
                                    view.onTasksLoadError(null)
                            }
                        }
                ) {
                    val view = view()
                    view?.onRefreshDone()
                }
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

    internal var showCompleted: Boolean
        get() = prefHelper.showCompleted
        set(showCompleted) {
            prefHelper.showCompleted = showCompleted
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
    }

    override fun unbindView(view: TasksView) {
        super.unbindView(view)
        realm.close()
    }
}
