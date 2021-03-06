package com.teo.ttasks.ui.fragments.task_lists

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.view.View.GONE
import android.view.WindowManager
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.teo.ttasks.R
import com.teo.ttasks.databinding.FragmentTaskListsBinding
import com.teo.ttasks.receivers.NetworkInfoReceiver.Companion.isOnline
import com.teo.ttasks.ui.activities.main.MainActivity
import com.teo.ttasks.ui.items.TaskListItem
import com.teo.ttasks.util.NightHelper
import com.teo.ttasks.util.toastShort
import eu.davidea.flexibleadapter.FlexibleAdapter
import org.koin.androidx.scope.lifecycleScope
import timber.log.Timber

class TaskListsFragment : DialogFragment(), TaskListsView {

    companion object {
        private const val EXTRA_TASK_LIST_ID = "taskListId"

        fun newInstance(taskListId: String? = null): TaskListsFragment = TaskListsFragment().apply {
            arguments = Bundle().apply {
                putString(EXTRA_TASK_LIST_ID, taskListId)
            }
        }
    }

    private val taskListsPresenter: TaskListsPresenter by lifecycleScope.inject()

    private lateinit var adapter: FlexibleAdapter<TaskListItem>

    private lateinit var taskListsBinding: FragmentTaskListsBinding

    private fun showDeleteTaskListDialog(taskListId: String) {
        AlertDialog.Builder(activity!!)
            .setTitle(R.string.delete_task_list)
            .setMessage(R.string.delete_task_list_message)
            .setPositiveButton(android.R.string.ok) { _, _ -> taskListsPresenter.deleteTaskList(taskListId) }
            .setNegativeButton(android.R.string.cancel) { _, _ -> }
            .show()
    }

    private fun showEditTaskListDialog(taskListItem: TaskListItem?) {
        val newTaskList = taskListItem == null

        val themeResId = if (NightHelper.isNight(context!!)) R.style.AlertDialog_AppCompat else R.style.AlertDialog_AppCompat_Light

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
            val context = context!!
            // TODO allow offline task creation
            if (!context.isOnline()) {
                Toast.makeText(context, "You must be online to be able to create a task list", Toast.LENGTH_SHORT).show()
            } else if (!taskListTitle.isEmpty()) {
                taskListsPresenter.setTaskListTitle(taskListTitle)
                if (taskListItem != null) {
                    taskListsPresenter.updateTaskList(taskListItem.id, context.isOnline())
                } else {
                    taskListsPresenter.createTaskList()
                }
                editDialog.dismiss()
            } else {
                context.toastShort(R.string.error_task_list_title_missing)
            }
        }

        // Set the task list title
        if (!newTaskList) {
            editDialog.findViewById<EditText>(R.id.task_list_title)!!.setText(taskListItem!!.title)
        }

        // Make sure the soft keyboard is displayed at the same time as the dialog
        editDialog.window!!.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
        editDialog.window!!.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        retainInstance = true
//        adapter = FlexibleAdapter(null)
//        adapter.addListener(FlexibleAdapter.OnItemClickListener { view, position ->
//            val item = adapter.getItem(position)!!
//            when (view.id) {
//                R.id.delete_task_list -> showDeleteTaskListDialog(item.id)
//                else -> showEditTaskListDialog(item)
//            }
//            return@OnItemClickListener true
//        })
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val taskListId = requireArguments().getString(EXTRA_TASK_LIST_ID)

        return MaterialAlertDialogBuilder(requireContext())
            .setView(R.layout.dialog_task_list_edit)
            .setTitle(if (taskListId == null) R.string.new_task_list else R.string.edit_task_list)
            .setPositiveButton(android.R.string.ok) { _, _ -> }
            .setNegativeButton(android.R.string.cancel) { _, _ -> }
            .create()
    }

    override fun onStart() {
        super.onStart()

        dialog?.window?.run {
            clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
            setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
        }

        dialog?.findViewById<View>(R.id.task_list_title)?.requestFocus()
    }

//    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
//        taskListsBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_task_lists, container, false)
//
//        taskListsBinding.taskLists.layoutManager = LinearLayoutManager(context)
//        taskListsBinding.taskLists.addItemDecoration(DividerItemDecoration(context!!, null))
//        taskListsBinding.taskLists.adapter = adapter
//
//        taskListsBinding.swipeRefreshLayout.setOnRefreshListener(this)
//
//        taskListsPresenter.bindView(this)
//        taskListsPresenter.getTaskLists()
//
//        return taskListsBinding.root
//    }

//    override fun onActivityCreated(savedInstanceState: Bundle?) {
//        super.onActivityCreated(savedInstanceState)
//        val fab = (activity as MainActivity).fab()
//        fab.setOnClickListener { showEditTaskListDialog(null) }
//    }

//    override fun onDestroyView() {
//        super.onDestroyView()
//        taskListsPresenter.unbindView(this)
//    }

    override fun onTaskListsLoading() {

    }

    override fun onTaskListsEmpty() {
        onRefreshDone()
    }

    override fun onTaskListsError() {
        onRefreshDone()
    }

    override fun onTaskListsLoaded(taskListItems: List<TaskListItem>) {
        Timber.d("Loaded ${taskListItems.size} task lists")
        onRefreshDone()
        adapter.updateDataSet(taskListItems)
        taskListsBinding.taskListsLoading.visibility = GONE
        taskListsBinding.taskListsLoadingError.visibility = GONE
        taskListsBinding.taskListsEmpty.visibility = GONE

        // Enable or disable scrolling depending on the amount of task lists to be displayed
        taskListsBinding.taskLists.post {
            val layoutManager = taskListsBinding.taskLists.layoutManager as LinearLayoutManager
            val position = layoutManager.findLastVisibleItemPosition()

            // Check if all task lists fit on the screen, in which case no need for scrolling
            val scrolling = !(adapter.mainItemCount - 1 <= position || position == RecyclerView.NO_POSITION)
            (activity as MainActivity).setAppBarScrolling(scrolling)
        }
    }

    override fun onTaskListUpdateError() {

    }

    fun onRefreshDone() {
        taskListsBinding.swipeRefreshLayout.isRefreshing = false
    }
}
