package com.teo.ttasks.ui.fragments.tasks

import com.google.firebase.database.FirebaseDatabase
import com.teo.ttasks.data.local.PrefHelper
import com.teo.ttasks.data.model.Task
import com.teo.ttasks.data.remote.TasksHelper
import com.teo.ttasks.ui.base.Presenter
import com.teo.ttasks.util.FirebaseUtil.getTasksDatabase
import com.teo.ttasks.util.SortType
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.processors.PublishProcessor
import io.realm.Realm

internal class TasksPresenter(
    private val tasksHelper: TasksHelper,
    private val prefHelper: PrefHelper
) : Presenter<TasksView>() {

    private var sortingMode = prefHelper.sortMode

    private val reminderProcessor = PublishProcessor.create<Pair<Task, String>>()

    /**
     * Map of task IDs to their respective reminder disposable.
     * Used to keep the list of reminder updates synchronized with the tasks on add/delete.
     */
    private val reminderMap = hashMapOf<String, Disposable>()

    private val reminderDisposables = CompositeDisposable()

    private val tasksDatabase by lazy { FirebaseDatabase.getInstance().getTasksDatabase() }

    private lateinit var realm: Realm

    internal var showCompleted: Boolean
        get() = prefHelper.showCompleted
        set(showCompleted) {
            prefHelper.showCompleted = showCompleted
        }

    /**
     * Load and monitor changes to the tasks associated with the provided
     * task list from the local database.
     *
     * @param taskListId task list identifier
     */
    internal fun subscribeToTasks(taskListId: String) {

//            .doOnNext { tasks ->
//                Timber.v(Thread.currentThread().name)
//                val taskMap = tasks.associateBy({ it.id }, { it })
//
//                // Dispose reminders for deleted tasks
//                val deletedTaskIds = reminderMap.keys - taskMap.keys
//                for (id in deletedTaskIds) {
//                    // Remove and dispose
//                    reminderMap.remove(id)?.let { reminderDisposables.remove(it) }
////                    Timber.v("Removed listener for task $id")
//                }
//
//                // Add reminder listeners for new tasks
//                val newTaskIds = taskMap.keys - reminderMap.keys
//                for (id in newTaskIds) {
//                    val reminderDisposable = tasksDatabase.reminder(id)!!.dataChanges()
//                        .retry { retryCount, throwable ->
//                            Timber.e(throwable, "Error while processing reminder for $id")
//                            return@retry retryCount <= 10
//                        }
//                        .subscribeOn(Schedulers.io())
//                        .subscribe({ dataSnapshot ->
//                            dataSnapshot?.getValue(String::class.java)?.let { date ->
//                                // Timber.v("Reminder for $id: $dateInMillis")
//                                val task = taskMap.getValue(id)
//                                if (task.reminder != date) {
//                                    reminderProcessor.onNext(Pair(task, date))
//                                }
//                            }
//                        }, { throwable ->
//                            Timber.e(throwable, "Fatal error while processing reminder for $id")
//                        })
//                    reminderDisposables.add(reminderDisposable)
//                    reminderMap[id] = reminderDisposable
////                    Timber.v("Added listener for task $id")
//                }
//            }
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
//        // TODO: optimize this since it still runs in the background...
//        val disposable = reminderProcessor
//            .observeOn(AndroidSchedulers.mainThread())
//            .subscribe(
//                { taskReminderPairs ->
//                    realm.executeTransaction {
//                        val (task, reminder) = taskReminderPairs
//                        task.reminder = reminder
//                    }
//                },
//                { Timber.e(it, "Error while processing the reminders") }
//            )
//        disposeOnUnbindView(disposable)
    }

    override fun unbindView(view: TasksView) {
        reminderDisposables.clear()
        reminderMap.clear()
        realm.close()
        super.unbindView(view)
    }
}
