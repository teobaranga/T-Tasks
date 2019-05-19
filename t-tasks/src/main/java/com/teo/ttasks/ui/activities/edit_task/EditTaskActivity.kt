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
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import com.teo.ttasks.R
import com.teo.ttasks.data.TaskListsAdapter
import com.teo.ttasks.data.model.Task
import com.teo.ttasks.data.model.TaskList
import com.teo.ttasks.databinding.ActivityEditTaskBinding
import com.teo.ttasks.util.DateUtils
import com.teo.ttasks.util.toastShort
import org.koin.android.scope.currentScope
import org.threeten.bp.LocalDateTime
import org.threeten.bp.LocalTime
import org.threeten.bp.ZoneId
import java.util.*

class EditTaskActivity : AppCompatActivity(), EditTaskView {

    class DatePickerFragment : DialogFragment() {
        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            // Use the current date as the default date in the picker
            val c = Calendar.getInstance()
            val year = c.get(Calendar.YEAR)
            val month = c.get(Calendar.MONTH)
            val day = c.get(Calendar.DAY_OF_MONTH)

            // Create a new instance of DatePickerDialog and return it
            return DatePickerDialog(requireContext(), activity as EditTaskActivity, year, month, day)
        }
    }

    private val editTaskPresenter: EditTaskPresenter by currentScope.inject()

    private lateinit var editTaskBinding: ActivityEditTaskBinding

    private lateinit var taskListsAdapter: TaskListsAdapter

    private lateinit var inputMethodManager: InputMethodManager

    private lateinit var taskListId: String

    private var datePickerFragment: DatePickerFragment? = null

    /**
     * Flag indicating that the reminder time has been clicked.
     * Used to differentiate between the reminder time and the due time.
     */
    private var reminderTimeClicked: Boolean = false

    /** Listener invoked when the reminder time has been selected */
    private val reminderTimeSetListener: TimePickerDialog.OnTimeSetListener =
        TimePickerDialog.OnTimeSetListener { _, hourOfDay, minute ->

            val localTime = LocalTime.of(hourOfDay, minute)

            val formattedTime = localTime.format(DateUtils.formatterTime)

            if (reminderTimeClicked) {
                editTaskBinding.reminder.text = formattedTime
                editTaskPresenter.setReminderTime(localTime)
                reminderTimeClicked = false
            } else {
//                editTaskBinding.dueDate.text = DateUtils.formatDate(this, time)
                editTaskBinding.dueTime.text = formattedTime
                editTaskPresenter.setDueTime(localTime)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

        editTaskBinding = DataBindingUtil.setContentView(this, R.layout.activity_edit_task)
        editTaskBinding.view = this
        editTaskPresenter.bindView(this)

        val taskId = intent.getStringExtra(EXTRA_TASK_ID)?.trim()
        taskListId = checkNotNull(intent.getStringExtra(EXTRA_TASK_LIST_ID)?.trim())

        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        taskListsAdapter = TaskListsAdapter(this).apply {
            setDropDownViewResource(R.layout.spinner_item_task_list_edit_dropdown)
            editTaskBinding.taskLists.adapter = this
        }

        if (taskId.isNullOrBlank()) {
            // Update the toolbar title
            supportActionBar!!.setTitle(R.string.title_activity_new_task)

            // Show the keyboard
            editTaskBinding.taskTitle.requestFocus()
            inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
        }

        // Load the available task lists and the task, if available
        editTaskPresenter.loadTask(taskListId, taskId)
    }

    override fun onDestroy() {
        editTaskPresenter.unbindView(this)
        super.onDestroy()
    }

    override fun onTaskLoaded(task: Task) {
        editTaskBinding.task = task
    }

    override fun onTaskListsLoaded(taskLists: List<TaskList>) {
        with(taskListsAdapter) {
            clear()
            addAll(taskLists)
        }

        if (taskLists.isNotEmpty()) {
            editTaskBinding.taskLists.setSelection(
                taskLists
                    .indexOfFirst { taskList -> taskList.id == taskListId }
                    .coerceAtLeast(0))
        }
    }

    override fun onTaskLoadError() {
        toastShort(R.string.error_task_loading)
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
     *
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
        val dateTime = LocalDateTime.of(year, monthOfYear + 1, dayOfMonth, 0, 0)
            .atZone(ZoneId.systemDefault())
        editTaskPresenter.dueDate = dateTime
        // Display the date after being processed by the presenter
        editTaskBinding.dueDate.text = dateTime.format(DateUtils.formatterDate)
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
                editTaskPresenter.finishTask()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    @Suppress("UNUSED_PARAMETER")
    fun onDueDateClicked(v: View) {
        // Hide the keyboard
        currentFocus?.let { inputMethodManager.hideSoftInputFromWindow(it.windowToken, 0) }

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
                datePickerFragment = (datePickerFragment ?: DatePickerFragment()).apply {
                    show(supportFragmentManager, "datePicker")
                }
                dialog.dismiss()
            }
        } else {
            datePickerFragment = (datePickerFragment ?: DatePickerFragment()).apply {
                show(supportFragmentManager, "datePicker")
            }
        }
    }

    @Suppress("UNUSED_PARAMETER")
    fun onDueTimeClicked(v: View) {
        currentFocus?.let { inputMethodManager.hideSoftInputFromWindow(it.windowToken, 0) }
        showReminderTimePickerDialog()
    }

    @Suppress("UNUSED_PARAMETER")
    fun onReminderClicked(v: View) {
        if (!editTaskPresenter.hasDueDate()) {
            toastShort("You need to set a due date before adding a reminder")
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

    /**
     * Show the picker for the task reminder time
     */
    private fun showReminderTimePickerDialog() {
        // Use the current time as the default values for the picker
        val c = Calendar.getInstance()
        val hour = c.get(Calendar.HOUR_OF_DAY)
        val minute = c.get(Calendar.MINUTE)

        // Create a new instance of TimePickerDialog and return it
        TimePickerDialog(
            this,
            reminderTimeSetListener,
            hour,
            minute,
            DateFormat.is24HourFormat(this)
        ).show()
    }

    companion object {

        private const val EXTRA_TASK_ID = "taskId"

        private const val EXTRA_TASK_LIST_ID = "taskListId"

        fun startEdit(context: Context, taskId: String, taskListId: String, bundle: Bundle?) {
            context.startActivity(getTaskCreateIntent(context, taskListId).apply {
                putExtra(EXTRA_TASK_ID, taskId)
            }, bundle)
        }

        fun startCreate(context: Context, taskListId: String, bundle: Bundle?) {
            context.startActivity(getTaskCreateIntent(context, taskListId), bundle)
        }

        /**
         * Used when starting this activity from the widget
         */
        fun getTaskCreateIntent(context: Context, taskListId: String): Intent {
            return Intent(context, EditTaskActivity::class.java).apply {
                putExtra(EXTRA_TASK_LIST_ID, taskListId)
            }
        }
    }
}
