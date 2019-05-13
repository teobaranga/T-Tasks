package com.teo.ttasks.ui.fragments.tasks

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.teo.ttasks.data.model.Task

class TasksViewModel: ViewModel() {

    val activeTasks = MutableLiveData<List<Task>>()

    val completedTasks = MutableLiveData<List<Task>>()
}
