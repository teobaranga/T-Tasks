package com.teo.ttasks.ui.fragments.task_lists

import android.content.DialogInterface
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.app.AlertDialog
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.EditText
import android.widget.Toast
import com.teo.ttasks.R
import com.teo.ttasks.databinding.FragmentTaskListsBinding
import com.teo.ttasks.receivers.NetworkInfoReceiver
import com.teo.ttasks.ui.DividerItemDecoration
import com.teo.ttasks.ui.activities.main.MainActivity
import com.teo.ttasks.ui.items.TaskListItem
import com.teo.ttasks.util.NightHelper
import dagger.android.support.DaggerFragment
import eu.davidea.flexibleadapter.FlexibleAdapter
import timber.log.Timber
import javax.inject.Inject

class TaskListsFragment : DaggerFragment(), TaskListsView, SwipeRefreshLayout.OnRefreshListener {

    @Inject internal lateinit var networkInfoReceiver: NetworkInfoReceiver
    @Inject internal lateinit var taskListsPresenter: TaskListsPresenter

    private lateinit var adapter: FlexibleAdapter<TaskListItem>

    private lateinit var taskListsBinding: FragmentTaskListsBinding

    internal fun showDeleteTaskListDialog(taskListId: String) {
        AlertDialog.Builder(activity!!)
                .setTitle(R.string.delete_task_list)
                .setMessage(R.string.delete_task_list_message)
                .setPositiveButton(android.R.string.ok) { _, _ -> taskListsPresenter.deleteTaskList(taskListId) }
                .setNegativeButton(android.R.string.cancel) { _, _ -> }
                .show()
    }

    private fun showEditTaskListDialog(taskListItem: TaskListItem?) {
        val newTaskList = taskListItem == null

        val themeResId = if (NightHelper.isNight(context!!)) R.style.MaterialBaseTheme_AlertDialog else R.style.MaterialBaseTheme_Light_AlertDialog

        val editDialog = AlertDialog.Builder(activity!!, themeResId)
                .setView(R.layout.dialog_task_list_edit)
                .setTitle(if (newTaskList) R.string.new_task_list else R.string.edit_task_list)
                .setPositiveButton(android.R.string.ok) { _, _ -> }
                .setNegativeButton(android.R.string.cancel) { _, _ -> }
                .show()

        // Create the task list if the title is valid
        editDialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
            val title = editDialog.findViewById<EditText>(R.id.task_list_title)!!
            val taskListTitle = title.text.toString()
            val context = context
            if (!networkInfoReceiver.isOnline(context)) {
                Toast.makeText(context, "You must be online to be able to create a task list", Toast.LENGTH_SHORT).show()
            } else if (!taskListTitle.isEmpty()) {
                taskListsPresenter.setTaskListTitle(taskListTitle)
                if (taskListItem != null) {
                    taskListsPresenter.updateTaskList(taskListItem.id!!, networkInfoReceiver.isOnline(context))
                } else {
                    taskListsPresenter.createTaskList()
                }
                editDialog.dismiss()
            } else {
                Toast.makeText(context, R.string.error_task_list_title_missing, Toast.LENGTH_SHORT).show()
            }
        }

        // Set the task list title
        if (!newTaskList)

            editDialog.findViewById<EditText>(R.id.task_list_title)!!.setText(taskListItem!!.title)

        // Make sure the soft keyboard is displayed at the same time as the dialog

        editDialog.window!!.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
        editDialog.window!!.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        adapter = FlexibleAdapter(null)
        adapter.addListener(FlexibleAdapter.OnItemClickListener { position ->
            val item = adapter.getItem(position)
            showEditTaskListDialog(item)
            true
        })

        // Handle delete button click
//        fastAdapter.withEventHook(object : ClickEventHook<TaskListItem>() {
//            override fun onBind(viewHolder: RecyclerView.ViewHolder): View? {
//                //return the views on which you want to bind this event
//                if (viewHolder is TaskListItem.ViewHolder) {
//                    return viewHolder.itemTaskListBinding.deleteTaskList
//                }
//                return null
//            }
//
//            override fun onClick(v: View, position: Int, fastAdapter: FastAdapter<TaskListItem>, item: TaskListItem) {
//                showDeleteTaskListDialog(item.id!!)
//            }
//        })
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        taskListsBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_task_lists, container, false)

        taskListsBinding.taskLists.layoutManager = LinearLayoutManager(context)
        taskListsBinding.taskLists.addItemDecoration(DividerItemDecoration(context!!, null))
        taskListsBinding.taskLists.adapter = adapter

        taskListsBinding.swipeRefreshLayout.setOnRefreshListener(this)

        taskListsPresenter.bindView(this)
        taskListsPresenter.getTaskLists()

        return taskListsBinding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val fab = (activity as MainActivity).fab()
        fab.setOnClickListener { showEditTaskListDialog(null) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        taskListsPresenter.unbindView(this)
    }

    override fun onRefresh() {
        // TODO: 2016-08-16 implement
        taskListsBinding.swipeRefreshLayout.isRefreshing = false
    }

    override fun onTaskListsLoading() {

    }

    override fun onTaskListsEmpty() {

    }

    override fun onTaskListsError() {

    }

    override fun onTaskListsLoaded(taskListItems: List<TaskListItem>) {
        Timber.d("Loaded ${taskListItems.size} task lists")
        adapter.clear()
        adapter.addItems(0, taskListItems)
        taskListsBinding.taskListsLoading.visibility = GONE
        taskListsBinding.taskListsLoadingError.visibility = GONE
        taskListsBinding.taskListsEmpty.visibility = GONE

        // Enable or disable scrolling depending on the amount of task lists to be displayed
        taskListsBinding.taskLists.post {
            val layoutManager = taskListsBinding.taskLists.layoutManager as LinearLayoutManager
            val position = layoutManager.findLastVisibleItemPosition()
            if (adapter.mainItemCount - 1 <= position || position == RecyclerView.NO_POSITION) {
                // All task lists fit on the screen, no need for scrolling
                (activity as MainActivity).disableScrolling(true)
            } else {
                (activity as MainActivity).enableScrolling()
            }
        }
    }

    override fun onTaskListUpdateError() {

    }

    companion object {
        fun newInstance(): TaskListsFragment = TaskListsFragment()
    }
}
