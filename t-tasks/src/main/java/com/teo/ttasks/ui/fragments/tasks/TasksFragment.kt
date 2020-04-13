package com.teo.ttasks.ui.fragments.tasks

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.os.Bundle
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.teo.ttasks.R
import com.teo.ttasks.TasksAdapter
import com.teo.ttasks.data.model.Task
import com.teo.ttasks.databinding.FragmentTasksBinding
import com.teo.ttasks.receivers.NetworkInfoReceiver
import com.teo.ttasks.receivers.NetworkInfoReceiver.Companion.isOnline
import com.teo.ttasks.ui.activities.main.MainActivity
import com.teo.ttasks.ui.activities.main.MainViewModel
import com.teo.ttasks.ui.task_detail.TaskDetailFragment
import com.teo.ttasks.util.toastShort
import org.koin.android.ext.android.inject
import org.koin.android.scope.lifecycleScope
import org.koin.android.viewmodel.ext.android.sharedViewModel
import org.koin.android.viewmodel.scope.viewModel
import timber.log.Timber

private const val RC_USER_RECOVERABLE = 1

class TasksFragment : Fragment(), TasksView {

    private val networkInfoReceiver: NetworkInfoReceiver by inject()

    private lateinit var tasksAdapter: TasksAdapter

    private lateinit var tasksBinding: FragmentTasksBinding

    private val tasksViewModel by lifecycleScope.viewModel<TasksViewModel>(this)

    private val mainViewModel by sharedViewModel<MainViewModel>()

    private val refreshListener: SwipeRefreshLayout.OnRefreshListener = SwipeRefreshLayout.OnRefreshListener {
        refreshTasks()
    }

    private val taskClickListener: TasksAdapter.TaskClickListener = object : TasksAdapter.TaskClickListener {
        // Reject quick, successive clicks
        private val MIN_CLICK_INTERVAL = 1_000
        private var lastClickTime = 0L

        override fun onTaskClicked(task: Task) {
            val currentTime = SystemClock.elapsedRealtime()
            if (currentTime - lastClickTime > MIN_CLICK_INTERVAL) {
                lastClickTime = currentTime
                TaskDetailFragment.newInstance(task.id, task.taskListId).show(parentFragmentManager, null)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            RC_USER_RECOVERABLE -> {
                if (resultCode == RESULT_OK) {
                    // Re-authorization successful, sync & refresh the tasks
                    tasksViewModel.syncTasks()
                    return
                }
                Toast.makeText(context, R.string.error_google_permissions_denied, Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true

        tasksAdapter = TasksAdapter().let {
            it.taskClickListener = taskClickListener
            return@let it
        }

        networkInfoReceiver.setOnConnectionChangedListener { isOnline ->
            if (isOnline) {
                Timber.d("isOnline")
                // Sync tasks
                tasksViewModel.syncTasks()
            }
        }

        mainViewModel.activeTaskList.observe(this, Observer {
            it?.let {
                Timber.v("Active task list: $it")
            }
        })
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        requireContext().registerReceiver(networkInfoReceiver, IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))
        tasksBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_tasks, container, false)
        return tasksBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tasksBinding.tasksList.apply {
            adapter = this@TasksFragment.tasksAdapter
        }

        tasksBinding.swipeRefreshLayout.setOnRefreshListener(refreshListener)

        tasksViewModel.activeTasks.observe(viewLifecycleOwner, Observer { activeTasks ->
            tasksAdapter.activeTasks = activeTasks
            tasksAdapter.notifyDataSetChanged()
            showTaskListIfNeeded()
            onTasksLoaded()
            Timber.v("Loaded %d active tasks", activeTasks.size)
        })

        tasksViewModel.completedTasks.observe(viewLifecycleOwner, Observer { completedTasks ->
            tasksAdapter.completedTasks = completedTasks
            tasksAdapter.notifyDataSetChanged()
            showTaskListIfNeeded()
            onTasksLoaded()
            Timber.v("Loaded %d completed tasks", completedTasks.size)
        })

        // Synchronize tasks and then refresh this task list
        refreshTasks()
    }

    override fun onDestroyView() {
        requireContext().unregisterReceiver(networkInfoReceiver)
        super.onDestroyView()
    }

    override fun onTasksLoading() {
        if (tasksAdapter.itemCount == 0) {
            with(tasksBinding) {
                tasksList.visibility = GONE
//                tasksLoading.visibility = VISIBLE
                tasksEmpty.visibility = GONE
            }
        }
    }

    override fun onTasksLoadError() {
        context?.toastShort(R.string.error_tasks_loading)
        onRefreshDone()
    }

    override fun onTasksEmpty() {
        tasksBinding.tasksList.visibility = GONE
//        tasksBinding.tasksLoading.visibility = GONE
        tasksBinding.tasksEmpty.visibility = VISIBLE
        onRefreshDone()
    }

    private fun showTaskListIfNeeded() {
        if (!tasksBinding.tasksList.isVisible) {
            with(tasksBinding) {
                tasksList.visibility = VISIBLE
//                tasksLoading.visibility = GONE
                tasksEmpty.visibility = GONE
            }
        }
    }

    override fun onTasksLoaded() {
        onRefreshDone()

        // Check if all tasks fit on the screen, in which case no need for scrolling
        val layoutManager = tasksBinding.tasksList.layoutManager as LinearLayoutManager
        val position = layoutManager.findLastVisibleItemPosition()
        val scrolling = !(tasksAdapter.itemCount - 1 <= position || position == RecyclerView.NO_POSITION)
        (activity as MainActivity).setAppBarScrolling(scrolling)
    }

    override fun onRefreshDone() {
        tasksBinding.swipeRefreshLayout.isRefreshing = false
    }

    override fun onSyncDone(taskSyncCount: Long) {
        if (taskSyncCount != 0L) {
            context?.toastShort("Synchronized $taskSyncCount tasks")
        }
        tasksViewModel.refreshTasks()
    }

    /**
     * Trigger the refresh process if an active network connection is available.
     */
    private fun refreshTasks() {
        if (!context.isOnline()) {
            onRefreshDone()
        } else {
            tasksViewModel.syncTasks()
        }
    }

    companion object {
        /** Create a new instance of this fragment */
        fun newInstance(): TasksFragment {
            return TasksFragment()
        }
    }
}
