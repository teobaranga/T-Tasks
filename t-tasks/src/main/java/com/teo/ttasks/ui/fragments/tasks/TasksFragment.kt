package com.teo.ttasks.ui.fragments.tasks

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.content.IntentFilter
import android.databinding.DataBindingUtil
import android.net.ConnectivityManager
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.support.design.widget.FloatingActionButton
import android.support.v4.app.ActivityOptionsCompat
import android.support.v4.util.Pair
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.SimpleItemAnimator
import android.view.*
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.Window.NAVIGATION_BAR_BACKGROUND_TRANSITION_NAME
import android.widget.Toast
import com.teo.ttasks.R
import com.teo.ttasks.databinding.FragmentTasksBinding
import com.teo.ttasks.receivers.NetworkInfoReceiver
import com.teo.ttasks.ui.activities.edit_task.EditTaskActivity
import com.teo.ttasks.ui.activities.main.MainActivity
import com.teo.ttasks.ui.activities.task_detail.TaskDetailActivity
import com.teo.ttasks.ui.items.CategoryItem
import com.teo.ttasks.ui.items.TaskItem
import com.teo.ttasks.util.RxUtils
import dagger.android.support.DaggerFragment
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.IFlexible
import timber.log.Timber
import java.util.*
import javax.inject.Inject

class TasksFragment : DaggerFragment(), TasksView, SwipeRefreshLayout.OnRefreshListener {

    /**
     * Array holding the 3 shared elements used during the transition to the [TaskDetailActivity].<br></br>
     * Indexes:<br></br>
     * 0 - task header layout, always present<br></br>
     * 1 - Navigation bar, can be missing<br></br>
     * 2 - FAB, could be hidden<br></br>
     */
    internal val pairs: Array<Pair<View, String>?> = arrayOfNulls(3)

    @Inject internal lateinit var tasksPresenter: TasksPresenter

    internal var taskListId: String? = null

    internal lateinit var tasksBinding: FragmentTasksBinding

    /**
     * The navigation bar view along with its associated transition name, used as a shared
     * element when selecting a task to prevent the task item from overlapping
     * it during the animation<br></br><br></br>
     * This is cached to avoid the `findViewById` every time a task is selected.<br></br>
     * **Note:** The view can be null if the activity is re-created after getting killed and
     * if that's the case [.createNavBarPair] must be called to recreate it.

     * @see [Shared elements overflow navigation bar in transition animation](http://stackoverflow.com/q/32501024/5606622)
     */
    internal lateinit var navBar: Pair<View, String>

    internal lateinit var fab: FloatingActionButton
    internal lateinit var adapter: FlexibleAdapter<IFlexible<*>>

    private lateinit var activeTasksHeader: CategoryItem
    private lateinit var completedTasksHeader: CategoryItem

    private val taskItemClickListener = object : FlexibleAdapter.OnItemClickListener {
        // Reject quick, successive clicks because they break the app
        private val MIN_CLICK_INTERVAL: Long = 1000
        private var lastClickTime: Long = 0

        override fun onItemClick(view: View, position: Int): Boolean {
            val item = adapter.getItem(position)!!
            if (item is CategoryItem && item === completedTasksHeader) {
                if (item.hasSubItems()) {
                    val showCompleted = !item.isExpanded
                    if (showCompleted) {
                        item.withTitle(String.format(getString(R.string.completed_count), item.subItemsCount))
                    } else {
                        item.withTitle(R.string.completed)
                    }
                    item.toggleArrow(true)
                    tasksPresenter.showCompleted = !showCompleted
                    adapter.notifyItemChanged(position)
                }
            } else if (item is TaskItem) {
                // Handle click on a task item
                val currentTime = SystemClock.elapsedRealtime()
                if (currentTime - lastClickTime > MIN_CLICK_INTERVAL) {
                    lastClickTime = currentTime

                    // Make sure the navigation bar view isn't null
                    if (navBar.first == null)
                        createNavBarPair()

                    // Add the task header layout to the shared elements
                    pairs[0] = Pair.create<View, String>(item.binding.layoutTask, getString(R.string.transition_task_header))

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
            return true
        }
    }
    private lateinit var networkInfoReceiver: NetworkInfoReceiver

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

        savedInstanceState?.let { taskListId = it.getString(ARG_TASK_LIST_ID) }

        activeTasksHeader = CategoryItem()
                .withTitle("Active")
        completedTasksHeader = CategoryItem()
                .withTitle(R.string.completed)

        adapter = FlexibleAdapter(Arrays.asList(activeTasksHeader, completedTasksHeader) as List<IFlexible<*>>)
        adapter.isAutoScrollOnExpand = false
        adapter.addListener(taskItemClickListener)

        createNavBarPair()

        networkInfoReceiver = NetworkInfoReceiver()
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

        tasksBinding.tasksList.layoutManager = LinearLayoutManager(context)
        tasksBinding.tasksList.adapter = adapter
        (tasksBinding.tasksList.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false

        tasksBinding.swipeRefreshLayout.setOnRefreshListener(this)

        if (!taskListId.isNullOrBlank()) {
            tasksPresenter.getTasks(taskListId!!)
        }

        // Synchronize tasks and then refresh this task list
//        refreshTasks()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        fab = (activity as MainActivity).fab()
        fab.setOnClickListener { EditTaskActivity.startCreate(this, taskListId!!, null) }
        pairs[2] = Pair.create<View, String>(fab, getString(R.string.transition_fab))
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(ARG_TASK_LIST_ID, taskListId)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        tasksPresenter.unbindView(this)
        context!!.unregisterReceiver(networkInfoReceiver)
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater!!.inflate(R.menu.menu_tasks, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item!!.itemId) {
            R.id.menu_sort_due_date -> if (tasksPresenter.switchSortMode(RxUtils.SORT_DATE))
                tasksPresenter.getTasks(taskListId!!)
            R.id.menu_sort_alphabetical -> if (tasksPresenter.switchSortMode(RxUtils.SORT_ALPHA))
                tasksPresenter.getTasks(taskListId!!)
            R.id.menu_sort_my_order -> if (tasksPresenter.switchSortMode(RxUtils.SORT_MY_ORDER))
                tasksPresenter.getTasks(taskListId!!)
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onTasksLoading() {
        tasksBinding.tasksLoading.visibility = VISIBLE
        tasksBinding.tasksEmpty.visibility = GONE
    }

    override fun onActiveTasksLoaded(activeTasks: List<TaskItem>) {
        activeTasksHeader.subItems = activeTasks
        adapter.expand(activeTasksHeader)
    }

    override fun onCompletedTasksLoaded(completedTasks: List<TaskItem>) {
        completedTasksHeader.subItems = completedTasks
        if (tasksPresenter.showCompleted) {
            adapter.expand(completedTasksHeader)
//            Handler().post { completedTasksHeader.toggleArrow(false) }
        }
    }

    override fun onTasksLoadError(resolveIntent: Intent?) {
        if (resolveIntent != null) {
            startActivityForResult(resolveIntent, RC_USER_RECOVERABLE)
        } else {
            Toast.makeText(context, R.string.error_tasks_loading, Toast.LENGTH_SHORT).show()
        }
        onRefreshDone()
    }

    override fun onTasksEmpty() {
        adapter.clear()
        tasksBinding.tasksLoading.visibility = GONE
        tasksBinding.tasksEmpty.visibility = VISIBLE
        onRefreshDone()
    }

    override fun onTasksLoaded() {
        tasksBinding.tasksLoading.visibility = GONE
        tasksBinding.tasksEmpty.visibility = GONE
        onRefreshDone()

        tasksBinding.tasksList.post {
            val layoutManager = tasksBinding.tasksList.layoutManager as LinearLayoutManager
            val position = layoutManager.findLastVisibleItemPosition()
            if (adapter.itemCount - 1 <= position || position == RecyclerView.NO_POSITION) {
                (activity as MainActivity).disableScrolling(true)
            } else {
                (activity as MainActivity).enableScrolling()
            }
        }
    }

    override fun onRefreshDone() {
        tasksBinding.swipeRefreshLayout.isRefreshing = false
    }

    override fun onSyncDone(taskSyncCount: Int) {
        if (taskSyncCount != 0)
            Toast.makeText(context, "Synchronized $taskSyncCount tasks", Toast.LENGTH_SHORT).show()
        tasksPresenter.refreshTasks(taskListId)
    }

    override fun onRefresh() {
        refreshTasks()
    }

    /** Cache the Pair holding the navigation bar view and its associated transition name  */
    internal fun createNavBarPair() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val navBarView = activity!!.window.decorView.findViewById<View>(android.R.id.navigationBarBackground)
            navBar = Pair.create(navBarView, NAVIGATION_BAR_BACKGROUND_TRANSITION_NAME)
            pairs[1] = navBar
        }
    }

    /**
     * Trigger the refresh process on an active network connection.
     */
    private fun refreshTasks() {
        if (!networkInfoReceiver.isOnline(context)) {
            onRefreshDone()
        } else {
            tasksPresenter.syncTasks(taskListId)
        }
    }

    /**
     * Switch the task list associated with this fragment and reload the tasks.

     * @param newTaskListId task list identifier
     */
    fun setTaskList(newTaskListId: String) {
        if (newTaskListId != taskListId) {
            // Disable fragment scrolling only if attached to prevent crashing
            if (isAdded)
                (activity as MainActivity).disableScrolling(false)
            taskListId = newTaskListId

            tasksPresenter.getTasks(taskListId!!)
            refreshTasks()
        }
    }

    companion object {

        private const val ARG_TASK_LIST_ID = "taskListId"

        private const val RC_USER_RECOVERABLE = 1

        /** Create a new instance of this fragment */
        fun newInstance(): TasksFragment = TasksFragment()
    }
}
