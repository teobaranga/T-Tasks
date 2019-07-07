package com.teo.ttasks.ui.fragments.tasks

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import com.teo.ttasks.LiveRealmResults
import com.teo.ttasks.data.model.Task
import com.teo.ttasks.data.remote.TasksHelper
import com.teo.ttasks.ui.base.RealmViewModel
import org.koin.core.KoinComponent
import org.koin.core.inject

class TasksViewModel : RealmViewModel(), KoinComponent {

    private val tasksHelper: TasksHelper by inject()

    private val _activeTasks: MediatorLiveData<List<Task>> = MediatorLiveData()

    private val _completedTasks: MediatorLiveData<List<Task>> = MediatorLiveData()

    private var currentActiveTasksSource: LiveData<List<Task>>? = null

    private var currentCompletedTasksSource: LiveData<List<Task>>? = null

    val activeTasks: LiveData<List<Task>>
        get() = _activeTasks

    val completedTasks: LiveData<List<Task>>
        get() = _completedTasks

    fun getTasks(taskListId: String) {
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
