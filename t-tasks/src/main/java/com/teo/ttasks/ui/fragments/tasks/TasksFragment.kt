package com.teo.ttasks.ui.fragments.tasks

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.os.Bundle
import android.os.SystemClock
import android.view.*
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.view.Window.NAVIGATION_BAR_BACKGROUND_TRANSITION_NAME
import android.widget.Toast
import androidx.core.app.ActivityOptionsCompat
import androidx.core.util.Pair
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.teo.ttasks.R
import com.teo.ttasks.data.model.Task
import com.teo.ttasks.databinding.FragmentTasksBinding
import com.teo.ttasks.receivers.NetworkInfoReceiver
import com.teo.ttasks.receivers.NetworkInfoReceiver.Companion.isOnline
import com.teo.ttasks.ui.SpacesItemDecoration
import com.teo.ttasks.ui.activities.edit_task.EditTaskActivity
import com.teo.ttasks.ui.activities.main.MainActivity
import com.teo.ttasks.ui.activities.task_detail.TaskDetailActivity
import com.teo.ttasks.ui.items.CategoryItem
import com.teo.ttasks.ui.items.TaskItem
import com.teo.ttasks.ui.items.TaskSectionItem
import com.teo.ttasks.util.dpToPx
import com.teo.ttasks.util.toastShort
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.IFlexible
import org.koin.android.ext.android.inject
import org.koin.android.scope.currentScope
import timber.log.Timber
import java.util.Collections.emptyList

private const val ARG_TASK_LIST_ID = "taskListId"

private const val RC_USER_RECOVERABLE = 1

/**
 * Fragment that displays the list of tasks belonging to the provided [taskListId].
 *
 * The fragment is initially created without a task list ID. When the task list IDs are available,
 * switching can be done using [updateTaskListId], which will register a subscription listening for
 * updates to the tasks from that task list. A refresh is then triggered in order to make sure the
 * data is not stale.
 */
class TasksFragment : Fragment(), TasksView, SwipeRefreshLayout.OnRefreshListener {

    /**
     * Array holding the 3 shared elements used during the transition to the [TaskDetailActivity].
     *
     * **0**: Task header layout, always present - *mandatory*
     *
     * **1**: Navigation bar, can be missing - *optional*
     *
     * **2**: FAB, could be hidden - *optional*
     */
    private val pairs: Array<Pair<View, String>?> = arrayOfNulls(3)

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

    /**
     * The navigation bar view along with its associated transition name, used as a shared
     * element when selecting a task to prevent the task item from overlapping
     * it during the animation. This is cached to avoid the `findViewById` every time a task is
     * selected.
     *
     * **Note:** The view can be null if the activity is re-created after getting killed and
     * if that's the case [createNavBarPair] must be called to recreate it.
     *
     * For more information, see
     * [Shared elements overflow navigation bar in transition animation](http://stackoverflow.com/q/32501024/5606622).
     */
    internal lateinit var navBar: Pair<View, String>

    internal lateinit var adapter: FlexibleAdapter<IFlexible<*>>

    internal lateinit var tasksAdapter: FlexibleAdapter<TaskSectionItem>

    private lateinit var activeTasksHeader: CategoryItem

    private lateinit var completedTasksHeader: CategoryItem

    private lateinit var tasksBinding: FragmentTasksBinding

    private val taskItemClickListener = object : FlexibleAdapter.OnItemClickListener {
        // Reject quick, successive clicks because they break the app
        private val MIN_CLICK_INTERVAL = 1000
        private var lastClickTime = 0L

        override fun onItemClick(view: View, position: Int): Boolean {
            val item = adapter.getItem(position)!!
            when (item) {
                is CategoryItem -> {
                    if (item == completedTasksHeader && item.hasSubItems()) {
                        tasksPresenter.showCompleted = item.isExpanded
                    }
                    return true
                }
                is TaskItem -> {
                    // Handle click on a task item
                    val currentTime = SystemClock.elapsedRealtime()
                    if (currentTime - lastClickTime > MIN_CLICK_INTERVAL) {
                        lastClickTime = currentTime

                        // Make sure the navigation bar view isn't null
                        if (navBar.first == null) {
                            createNavBarPair()
                        }

                        // Add the task header layout to the shared elements

                        pairs[0] = Pair.create(item.binding.layoutTask, getString(R.string.transition_task_header))

                        // Find the shared elements to be used in the transition
                        val sharedElements: Array<Pair<View, String>?>

                        if (!fab.isShown) {
                            // Check the navigation bar view
                            sharedElements = if (pairs[1]?.first == null) {
                                // Get only the task header layout element
                                arrayOf(pairs[0])
                            } else {
                                // Get the task header layout and the navigation bar
                                arrayOf(pairs[0], pairs[1])
                            }
                        } else {
                            sharedElements = if (pairs[1]?.first == null) {
                                // Get only the task header and the FAB
                                arrayOf(pairs[0], pairs[2])
                            } else {
                                // Get all the 3 elements
                                pairs
                            }
                        }

                        val options = ActivityOptionsCompat.makeSceneTransitionAnimation(activity!!, *sharedElements)
                        TaskDetailActivity.start(context!!, item.taskId, taskListId!!, options.toBundle())
                    }
                }
            }
            return true
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
        setHasOptionsMenu(true)
        retainInstance = true

        savedInstanceState?.let { taskListId = it.getString(ARG_TASK_LIST_ID) }

        activeTasksHeader = CategoryItem(getString(R.string.active)).apply { isExpanded = true }
        completedTasksHeader = CategoryItem(getString(R.string.completed), getString(R.string.completed_count)).apply {
            isExpanded = tasksPresenter.showCompleted
        }

        adapter = FlexibleAdapter<IFlexible<*>>(null, null).apply {
            isAutoScrollOnExpand = false
            addListener(taskItemClickListener)
        }

        tasksAdapter = FlexibleAdapter(null)

        createNavBarPair()

        networkInfoReceiver.setOnConnectionChangedListener { isOnline ->
            if (isOnline) {
                Timber.d("isOnline")
                // Sync tasks
                tasksPresenter.syncTasks(taskListId)
            }
        }
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
            layoutManager = LinearLayoutManager(context)
            adapter = this@TasksFragment.tasksAdapter
            addItemDecoration(SpacesItemDecoration(8.dpToPx()))
            setHasFixedSize(true)
        }

        tasksBinding.swipeRefreshLayout.setOnRefreshListener(this)

        taskListId?.let { tasksPresenter.subscribeToTasks(it, savedInstanceState != null) }

        // Synchronize tasks and then refresh this task list
        refreshTasks()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val mainActivity = activity as MainActivity
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState.apply { putString(ARG_TASK_LIST_ID, taskListId) })
    }

    override fun onDestroyView() {
        context!!.unregisterReceiver(networkInfoReceiver)
        tasksPresenter.unbindView(this)
        super.onDestroyView()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_tasks, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val refresh: Boolean = when (item.itemId) {
//            R.id.menu_sort_due_date -> tasksPresenter.switchSortMode(SortType.SORT_DATE)
//            R.id.menu_sort_alphabetical -> tasksPresenter.switchSortMode(SortType.SORT_ALPHA)
//            R.id.menu_sort_my_order -> tasksPresenter.switchSortMode(SortType.SORT_CUSTOM)
            else -> false
        }
        if (refresh) {
            tasksPresenter.subscribeToTasks(taskListId!!)
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onTasksLoading() {
        tasksBinding.tasksLoading.visibility = VISIBLE
        tasksBinding.tasksEmpty.visibility = GONE
    }

    // TODO combine the code for loading the active/completed tasks
    override fun onActiveTasksLoaded(activeTasks: List<Task>) {
//        activeTasksHeader.subItems = activeTasks
        if (!activeTasksHeader.isExpanded) {
            adapter.expand(activeTasksHeader)
        }
        tasksAdapter.addItem(TaskSectionItem(R.drawable.ic_whatshot_24dp, R.string.active, activeTasks, TaskSectionItem.DateType.DUE))
        Timber.v("Loaded %d active tasks", activeTasks.size)
    }

    override fun onCompletedTasksLoaded(completedTasks: List<Task>) {
//        completedTasksHeader.subItems = completedTasks
        if (tasksPresenter.showCompleted && !completedTasksHeader.isExpanded) {
            adapter.expand(completedTasksHeader)
//                    Handler().post { completedTasksHeader.toggleArrow(false) }
        }
        tasksAdapter.addItem(TaskSectionItem(R.drawable.ic_done_white_24dp, R.string.completed, completedTasks, TaskSectionItem.DateType.COMPLETED))
        Timber.v("loaded %d completed tasks", completedTasks.size)
    }

    override fun onTasksLoadError() {
        context?.toastShort(R.string.error_tasks_loading)
        onRefreshDone()
    }

    override fun onTasksEmpty() {
        Timber.v("onTasksEmpty")
        adapter.updateDataSet(emptyList())
        tasksBinding.tasksList.visibility = GONE
        tasksBinding.tasksLoading.visibility = GONE
        tasksBinding.tasksEmpty.visibility = VISIBLE
        onRefreshDone()
    }

    override fun onTasksLoaded() {
        Timber.v("onTasksLoaded")
        val itemList = mutableListOf<CategoryItem>()
        if (activeTasksHeader.subItemsCount > 0) {
            itemList.add(activeTasksHeader)
        }
        if (completedTasksHeader.subItemsCount > 0) {
            itemList.add(completedTasksHeader)
        }
        adapter.updateDataSet(itemList.toList())
        tasksBinding.tasksList.visibility = VISIBLE
        tasksBinding.tasksLoading.visibility = GONE
        tasksBinding.tasksEmpty.visibility = GONE
        onRefreshDone()

        tasksBinding.tasksList.post {
            val layoutManager = tasksBinding.tasksList.layoutManager as LinearLayoutManager
            val position = layoutManager.findLastVisibleItemPosition()
            if (tasksAdapter.itemCount - 1 <= position || position == RecyclerView.NO_POSITION) {
                (activity as MainActivity).setFabScrolling(enable = false, delay = true)
            } else {
                (activity as MainActivity).setFabScrolling(true)
            }
        }
    }

    override fun onRefresh() {
        refreshTasks()
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

    /** Cache the Pair holding the navigation bar view and its associated transition name  */
    internal fun createNavBarPair() {
        val navBarView = activity!!.window.decorView.findViewById<View>(android.R.id.navigationBarBackground)
        navBar = Pair.create(navBarView, NAVIGATION_BAR_BACKGROUND_TRANSITION_NAME)
        pairs[1] = navBar
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
            tasksAdapter.clear()
            // In case it is attached, disable fragment scrolling to prevent crashing
            if (isAdded) {
                (activity as MainActivity).setFabScrolling(false)
            }
            taskListId = newTaskListId
            tasksPresenter.subscribeToTasks(taskListId!!)
            refreshTasks()
        }
    }

    companion object {
        /** Create a new instance of this fragment */
        fun newInstance(): TasksFragment = TasksFragment()
    }
}
