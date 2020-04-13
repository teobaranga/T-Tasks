package com.teo.ttasks.ui.fragments.tasks

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import com.teo.ttasks.LiveRealmResults
import com.teo.ttasks.data.model.Task
import com.teo.ttasks.data.remote.TasksHelper
import com.teo.ttasks.ui.base.RealmViewModel
import io.reactivex.android.schedulers.AndroidSchedulers
import org.koin.core.KoinComponent
import org.koin.core.inject
import timber.log.Timber

class TasksViewModel : RealmViewModel(), KoinComponent {

    private val tasksHelper: TasksHelper by inject()

    private val _activeTasks: MediatorLiveData<List<Task>> = MediatorLiveData()

    private val _completedTasks: MediatorLiveData<List<Task>> = MediatorLiveData()

    private var currentActiveTasksSource: LiveData<List<Task>>? = null

    private var currentCompletedTasksSource: LiveData<List<Task>>? = null

    var taskListId: String? = null
        set(value) {
            if (field != value && value != null) {
                getTasks(value)
            }
            field = value
        }

    val activeTasks: LiveData<List<Task>>
        get() = _activeTasks

    val completedTasks: LiveData<List<Task>>
        get() = _completedTasks

    internal fun refreshTasks() {
        taskListId?.let {
            val subscription = tasksHelper.refreshTasks(it)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    {
//                    view()?.onRefreshDone()
                    },
                    { throwable ->
                        Timber.e(throwable, "Error refreshing tasks")
//                    view()?.onTasksLoadError()
                    })
//        disposeOnUnbindView(subscription)
        }
    }

    /**
     * Synchronize the local tasks from the specified task list.
     * Tasks that have only been updated locally are uploaded to the Google servers.
     */
    internal fun syncTasks() {
        taskListId?.let {
            // Keep track of the number of synced tasks
            val subscription = tasksHelper.syncTasks(it)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    {
                        // Syncing done
//                    view()?.onSyncDone(it)
                    },
                    {
                        // Sync failed for at least one task, will retry on next refresh
                        Timber.e(it, "Error synchronizing tasks")
//                    view()?.onSyncDone(0)
                    }
                )
//        disposeOnUnbindView(subscription)
        }
    }

    private fun getTasks(taskListId: String) {
        with(LiveRealmResults(tasksHelper.getActiveTasks(taskListId, realm))) {
            currentActiveTasksSource?.let { _activeTasks.removeSource(it) }

            _activeTasks.addSource(this) {
                _activeTasks.value = it
            }

            currentActiveTasksSource = this
        }

        with(LiveRealmResults(tasksHelper.getCompletedTasks(taskListId, realm))) {
            currentCompletedTasksSource?.let { _completedTasks.removeSource(it) }

            _completedTasks.addSource(this) {
                _completedTasks.value = it
            }

            currentCompletedTasksSource = this
        }
    }
}
