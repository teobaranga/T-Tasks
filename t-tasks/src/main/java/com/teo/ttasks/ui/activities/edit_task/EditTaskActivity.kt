package com.teo.ttasks.ui.activities.edit_task

import android.app.DatePickerDialog
import android.app.Dialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.format.DateFormat
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.DatePicker
import android.widget.LinearLayout
import android.widget.TimePicker
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import com.mikepenz.materialdrawer.util.KeyboardUtil
import com.teo.ttasks.R
import com.teo.ttasks.data.TaskListsAdapter
import com.teo.ttasks.data.model.Task
import com.teo.ttasks.data.model.TaskList
import com.teo.ttasks.databinding.ActivityEditTaskBinding
import com.teo.ttasks.receivers.NetworkInfoReceiver
import com.teo.ttasks.receivers.NetworkInfoReceiver.Companion.isOnline
import com.teo.ttasks.util.DateUtils
import dagger.android.support.DaggerAppCompatActivity
import java.util.*
import javax.inject.Inject

class EditTaskActivity : DaggerAppCompatActivity(), EditTaskView {

    @Inject internal lateinit var editTaskPresenter: EditTaskPresenter
    @Inject internal lateinit var networkInfoReceiver: NetworkInfoReceiver

    private lateinit var editTaskBinding: ActivityEditTaskBinding

    private lateinit var taskListsAdapter: TaskListsAdapter

    private lateinit var taskListId: String

    private var datePickerFragment: DatePickerFragment? = null

    private var taskId: String? = null

    /** Listener invoked when the reminder time has been selected */
    private val reminderTimeSetListener: TimePickerDialog.OnTimeSetListener =
            TimePickerDialog.OnTimeSetListener { _, hourOfDay, minute ->
                val time = Calendar.getInstance()
                        .apply {
                            set(Calendar.HOUR_OF_DAY, hourOfDay)
                            set(Calendar.MINUTE, minute)
                        }
                        .time
                if (reminderTimeClicked) {
                    editTaskBinding.reminder.text = DateUtils.formatTime(this, time)
                    editTaskPresenter.setReminderTime(time)
                    reminderTimeClicked = false
                } else {
                    editTaskBinding.dueDate.text = DateUtils.formatDate(this, time)
                    editTaskBinding.dueTime.text = DateUtils.formatTime(this, time)
                    editTaskPresenter.setDueTime(time)
                }
            }

    /**
     * Flag indicating that the reminder time has been clicked.
     * Used to differentiate between the reminder time and the due time.
     */
    private var reminderTimeClicked: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        editTaskBinding = DataBindingUtil.setContentView(this, R.layout.activity_edit_task)
        editTaskBinding.view = this
        editTaskPresenter.bindView(this)

        taskId = intent.getStringExtra(EXTRA_TASK_ID)
        taskListId = intent.getStringExtra(EXTRA_TASK_LIST_ID)

        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        taskListsAdapter = TaskListsAdapter(this)
        taskListsAdapter.setDropDownViewResource(R.layout.spinner_item_task_list_edit_dropdown)
        editTaskBinding.taskLists.adapter = taskListsAdapter

        // Handle a new task or an existing task
        if (taskId.isNullOrBlank()) {
            // Update the toolbar title
            supportActionBar!!.setTitle(R.string.title_activity_new_task)

            // Show the keyboard
            editTaskBinding.taskTitle.requestFocus()
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(editTaskBinding.taskTitle, InputMethodManager.SHOW_IMPLICIT)
        } else {
            editTaskPresenter.loadTaskInfo(taskId!!)
        }

        // Load the available task lists
        editTaskPresenter.loadTaskLists(taskListId)
    }

    override fun onDestroy() {
        super.onDestroy()
        editTaskPresenter.unbindView(this)
    }

    override fun onTaskLoaded(task: Task) {
        editTaskBinding.task = task
    }

    override fun onTaskListsLoaded(taskLists: List<TaskList>, selectedPosition: Int) {
        taskListsAdapter.addAll(taskLists)
        editTaskBinding.taskLists.setSelection(selectedPosition)
    }

    override fun onTaskLoadError() {
        Toast.makeText(this, R.string.error_task_loading, Toast.LENGTH_SHORT).show()
        finish()
    }

    override fun onTaskSaved() {
        onBackPressed()
    }

    override fun onTaskSaveError() {
        // TODO: 2016-07-24 implement
    }

    /**
     * Reset the due time

     * @return true if the due time was reset, false otherwise
     */
    override fun onDueTimeLongClicked(v: View): Boolean {
        // TODO: 2016-05-18 return false if the due time is already reset
        editTaskBinding.dueTime.setText(R.string.due_time_all_day)
        return true
    }

    override fun onTitleChanged(title: CharSequence, start: Int, before: Int, count: Int) {
        editTaskPresenter.setTaskTitle(title.toString())
        // Clear the error
        editTaskBinding.taskTitle.error = null
    }

    override fun onNotesChanged(notes: CharSequence, start: Int, before: Int, count: Int) {
        editTaskPresenter.setTaskNotes(notes.toString())
    }

    override fun onDateSet(view: DatePicker, year: Int, monthOfYear: Int, dayOfMonth: Int) {
        val c = Calendar.getInstance()
        c.set(year, monthOfYear, dayOfMonth)
        val time = c.time
        editTaskPresenter.dueDate = time
        // Display the date after being processed by the presenter
        editTaskBinding.dueDate.text = DateUtils.formatDate(this, time)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_edit_task, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
            R.id.done -> {
                if (editTaskBinding.taskTitle.length() == 0) {
                    editTaskBinding.taskTitle.error = getString(R.string.error_no_title)
                    editTaskBinding.taskTitle.requestFocus()
                    val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.showSoftInput(editTaskBinding.taskTitle, InputMethodManager.SHOW_IMPLICIT)
                    return true
                }
                if (taskId.isNullOrBlank()) {
                    editTaskPresenter.newTask(taskListId)
                } else {
                    editTaskPresenter.updateTask(taskListId, taskId!!, isOnline())
                }
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    @Suppress("UNUSED_PARAMETER")
    fun onDueDateClicked(v: View) {
        KeyboardUtil.hideKeyboard(this)
        if (editTaskPresenter.hasDueDate()) {
            val dialog = AlertDialog.Builder(this)
                    .setView(R.layout.dialog_remove_change)
                    .show()


            dialog.findViewById<LinearLayout>(R.id.remove)!!.setOnClickListener {
                // Reset the due date & reminder
                editTaskPresenter.removeDueDate()
                editTaskPresenter.removeReminder()
                editTaskBinding.dueDate.text = null
                editTaskBinding.reminder.text = null
                dialog.dismiss()
            }

            dialog.findViewById<LinearLayout>(R.id.change)!!.setOnClickListener {
                datePickerFragment = datePickerFragment ?: DatePickerFragment()
                datePickerFragment!!.show(supportFragmentManager, "datePicker")
                dialog.dismiss()
            }
        } else {
            datePickerFragment = datePickerFragment ?: DatePickerFragment()
            datePickerFragment!!.show(supportFragmentManager, "datePicker")
        }
    }

    @Suppress("UNUSED_PARAMETER")
    fun onDueTimeClicked(v: View) {
        KeyboardUtil.hideKeyboard(this)
        showReminderTimePickerDialog()
    }

    @Suppress("UNUSED_PARAMETER")
    fun onReminderClicked(v: View) {
        if (!editTaskPresenter.hasDueDate()) {
            Toast.makeText(this, "You need to set a due date before adding a reminder", Toast.LENGTH_SHORT).show()
            return
        }
        if (editTaskPresenter.hasReminder()) {
            val dialog = AlertDialog.Builder(this)
                    .setView(R.layout.dialog_remove_change)
                    .show()


            dialog.findViewById<LinearLayout>(R.id.remove)!!.setOnClickListener {
                editTaskPresenter.removeReminder()
                editTaskBinding.reminder.text = null
                dialog.dismiss()
            }

            dialog.findViewById<LinearLayout>(R.id.change)!!.setOnClickListener {
                reminderTimeClicked = true
                showReminderTimePickerDialog()
                dialog.dismiss()
            }
        } else {
            reminderTimeClicked = true
            showReminderTimePickerDialog()
        }
    }

    class DatePickerFragment : DialogFragment() {
        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            // Use the current date as the default date in the picker
            val c = Calendar.getInstance()
            val year = c.get(Calendar.YEAR)
            val month = c.get(Calendar.MONTH)
            val day = c.get(Calendar.DAY_OF_MONTH)

            // Create a new instance of DatePickerDialog and return it
            return DatePickerDialog(context, activity as EditTaskActivity, year, month, day)
        }
    }

    /**
     * Show the picker for the task reminder time
     */
    private fun showReminderTimePickerDialog() {
        // Use the current time as the default values for the picker
        val c = Calendar.getInstance()
        val hour = c.get(Calendar.HOUR_OF_DAY)
        val minute = c.get(Calendar.MINUTE)

        // Create a new instance of TimePickerDialog and return it
        val timePickerDialog =  TimePickerDialog(this, reminderTimeSetListener, hour, minute, DateFormat.is24HourFormat(this))
        timePickerDialog.show()
    }

    companion object {

        private const val EXTRA_TASK_ID = "taskId"
        private const val EXTRA_TASK_LIST_ID = "taskListId"

        fun startEdit(context: Context, taskId: String, taskListId: String, bundle: Bundle?) {
            val starter = Intent(context, EditTaskActivity::class.java)
            starter.putExtra(EXTRA_TASK_ID, taskId)
            starter.putExtra(EXTRA_TASK_LIST_ID, taskListId)
            context.startActivity(starter, bundle)
        }

        fun startCreate(fragment: Fragment, taskListId: String, bundle: Bundle?) {
            val starter = Intent(fragment.context, EditTaskActivity::class.java)
            starter.putExtra(EXTRA_TASK_LIST_ID, taskListId)
            fragment.startActivity(starter, bundle)
        }

        /**
         * Used when starting this activity from the widget
         */
        fun getTaskCreateIntent(context: Context, taskListId: String): Intent {
            val starter = Intent(context, EditTaskActivity::class.java)
            starter.putExtra(EXTRA_TASK_LIST_ID, taskListId)
            return starter
        }
    }
}
