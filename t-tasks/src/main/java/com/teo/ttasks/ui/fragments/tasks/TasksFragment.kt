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
import androidx.lifecycle.ViewModelProviders
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
import com.teo.ttasks.ui.task_detail.TaskDetailFragment
import com.teo.ttasks.util.ARG_TASK_LIST_ID
import com.teo.ttasks.util.toastShort
import org.koin.android.ext.android.inject
import org.koin.android.scope.currentScope
import timber.log.Timber

private const val RC_USER_RECOVERABLE = 1

/**
 * Fragment that displays the list of tasks belonging to the provided [taskListId].
 *
 * The fragment is initially created without a task list ID. When the task list IDs are available,
 * switching can be done using [updateTaskListId], which will register a subscription listening for
 * updates to the tasks from that task list. A refresh is then triggered in order to make sure the
 * data is not stale.
 */
class TasksFragment : Fragment(), TasksView {

    private val tasksPresenter: TasksPresenter by currentScope.inject()

    private val networkInfoReceiver: NetworkInfoReceiver by inject()

    /** ID of the current task list. Its value is either null or a non-empty string. */
    internal var taskListId: String? = null
        set(value) {
            field = value?.trim()
            if (field?.isEmpty() == true) {
                field = null
            }
        }

    private lateinit var tasksAdapter: TasksAdapter

    private lateinit var tasksBinding: FragmentTasksBinding

    private lateinit var tasksViewModel: TasksViewModel

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
                TaskDetailFragment.newInstance(task.id, taskListId!!).show(fragmentManager!!, null)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            RC_USER_RECOVERABLE -> {
                if (resultCode == RESULT_OK) {
                    // Re-authorization successful, sync & refresh the tasks
                    tasksPresenter.syncTasks(taskListId)
                    return
                }
                Toast.makeText(context, R.string.error_google_permissions_denied, Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true

        taskListId = savedInstanceState?.getString(ARG_TASK_LIST_ID) ?: arguments?.getString(ARG_TASK_LIST_ID)

        tasksAdapter = TasksAdapter().let {
            it.taskClickListener = taskClickListener
            return@let it
        }

        networkInfoReceiver.setOnConnectionChangedListener { isOnline ->
            if (isOnline) {
                Timber.d("isOnline")
                // Sync tasks
                tasksPresenter.syncTasks(taskListId)
            }
        }

        tasksViewModel = ViewModelProviders.of(this)[TasksViewModel::class.java]
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        context!!.registerReceiver(networkInfoReceiver, IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))
        tasksBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_tasks, container, false)
        return tasksBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        tasksPresenter.bindView(this)

        tasksBinding.tasksList.apply {
            adapter = this@TasksFragment.tasksAdapter
        }

        tasksBinding.swipeRefreshLayout.setOnRefreshListener(refreshListener)

        taskListId?.let {
            tasksViewModel.activeTasks.observe(this, Observer { activeTasks ->
                if (activeTasks.isNotEmpty()) {
                    tasksAdapter.activeTasks = activeTasks
                    tasksAdapter.notifyDataSetChanged()
                    showTaskListIfNeeded()
                }
                onTasksLoaded()
                Timber.v("Loaded %d active tasks", activeTasks.size)
            })

            tasksViewModel.completedTasks.observe(this, Observer { completedTasks ->
                if (completedTasks.isNotEmpty()) {
                    tasksAdapter.completedTasks = completedTasks
                    tasksAdapter.notifyDataSetChanged()
                    showTaskListIfNeeded()
                }
                onTasksLoaded()
                Timber.v("Loaded %d completed tasks", completedTasks.size)
            })
            tasksViewModel.getTasks(it)
        }

        // Synchronize tasks and then refresh this task list
        refreshTasks()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState.apply { putString(ARG_TASK_LIST_ID, taskListId) })
    }

    override fun onDestroyView() {
        context!!.unregisterReceiver(networkInfoReceiver)
        tasksPresenter.unbindView(this)
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
        tasksPresenter.refreshTasks(taskListId)
    }

    /**
     * Trigger the refresh process if an active network connection is available.
     */
    private fun refreshTasks() {
        if (!context.isOnline()) {
            onRefreshDone()
        } else {
            tasksPresenter.syncTasks(taskListId)
        }
    }

    /**
     * Switch the task list associated with this fragment and reload the tasks.
     *
     * @param newTaskListId task list identifier
     */
    fun updateTaskListId(newTaskListId: String) {
        if (newTaskListId != taskListId) {
            taskListId = newTaskListId
            tasksViewModel.getTasks(taskListId!!)
            refreshTasks()
        }
    }

    companion object {
        /** Create a new instance of this fragment */
        fun newInstance(taskListId: String?): TasksFragment {
            val tasksFragment = TasksFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_TASK_LIST_ID, taskListId)
                }
            }
            Timber.v("New TasksFragment: ${tasksFragment.arguments}")
            return tasksFragment
        }
    }
}
