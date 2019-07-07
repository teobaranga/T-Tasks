package com.teo.ttasks.ui.task_detail

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.teo.ttasks.data.local.WidgetHelper
import com.teo.ttasks.data.model.Task
import com.teo.ttasks.data.model.TaskList
import com.teo.ttasks.data.remote.TasksHelper
import com.teo.ttasks.ui.base.RealmViewModel
import com.teo.ttasks.util.NotificationHelper
import org.koin.core.KoinComponent
import org.koin.core.inject

class TaskDetailViewModel : RealmViewModel(), KoinComponent {

    private val notificationHelper: NotificationHelper by inject()

    private val tasksHelper: TasksHelper by inject()

    private val widgetHelper: WidgetHelper by inject()

    private val _task: MutableLiveData<Task> = MutableLiveData()

    private val _taskList: MutableLiveData<TaskList> = MutableLiveData()

    val task: LiveData<Task>
        get() = _task

    val taskList: LiveData<TaskList>
        get() = _taskList

    fun loadTask(taskId: String) {
        val task = tasksHelper.getTask(taskId, realm)
        task?.addChangeListener { t: Task ->
            if (t.isLoaded && t.isValid) {
                _task.value = t
            }
        }
    }

    fun loadTaskList(taskListId: String) {
        val taskList = tasksHelper.getTaskList(taskListId, realm)
        taskList?.addChangeListener { t: TaskList ->
            if (t.isLoaded && t.isValid) {
                _taskList.value = t
            }
        }
    }

    fun updateCompletionStatus() {
        _task.value?.let {
            tasksHelper.updateCompletionStatus(it, realm)

            // Trigger a widget update
            widgetHelper.updateWidgets(it.taskListId)

            // Reschedule the reminder if going from completed -> active
            if (!it.isCompleted) {
                notificationHelper.scheduleTaskNotification(it)
            }
        }
    }

}
