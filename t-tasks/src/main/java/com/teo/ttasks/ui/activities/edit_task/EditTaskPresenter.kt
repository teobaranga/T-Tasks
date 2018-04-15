package com.teo.ttasks.ui.activities.edit_task

import android.support.v4.util.Pair
import com.google.firebase.database.FirebaseDatabase
import com.teo.ttasks.data.local.TaskFields
import com.teo.ttasks.data.local.WidgetHelper
import com.teo.ttasks.data.model.Task
import com.teo.ttasks.data.model.TaskList
import com.teo.ttasks.data.remote.TasksHelper
import com.teo.ttasks.jobs.CreateTaskJob
import com.teo.ttasks.ui.base.Presenter
import com.teo.ttasks.util.DateUtils.Companion.utcDateFormat
import com.teo.ttasks.util.FirebaseUtil.getTasksDatabase
import com.teo.ttasks.util.FirebaseUtil.saveReminder
import com.teo.ttasks.util.NotificationHelper
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.realm.Realm
import io.realm.RealmResults
import timber.log.Timber
import java.util.*

internal class EditTaskPresenter(private val tasksHelper: TasksHelper,
                                 private val widgetHelper: WidgetHelper,
                                 private val notificationHelper: NotificationHelper) : Presenter<EditTaskView>() {

    private lateinit var realm: Realm

    /** The due date */
    internal var dueDate: Date? = null
        set(value) {
            /**
             * Set the due date. If one isn't present, assign the new one. Otherwise, modify the old one.
             */
            if (value == null) {
                reminder = null
            } else {
                // Remove the time information
                val cal = Calendar.getInstance()
                cal.time = value
                cal.set(Calendar.HOUR, 0)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)

                field = cal.time

                // Update the reminder
                if (reminder != null) {
                    reminder = value
                }
            }
            editTaskFields.dueDate = utcDateFormat.format(value)
            Timber.v("Due date: %s", editTaskFields.dueDate)
        }

    private var reminder: Date? = null

    /** Object containing the task fields that have been modified. */
    private val editTaskFields: TaskFields = TaskFields()

    private var taskSubscription: Disposable? = null

    /**
     * Load the task and display its information into the view.
     *
     * @param taskId task list identifier
     */
    internal fun loadTaskInfo(taskId: String) {
        taskSubscription?.let { if (!it.isDisposed) it.dispose() }
        taskSubscription = tasksHelper.getTaskAsSingle(taskId, realm)
                .subscribe { task ->
                    dueDate = task.due
                    reminder = task.reminder
                    view()?.onTaskLoaded(task)
                }
    }

    /**
     * Load the task lists and find the index of the provided task list in it.
     * This is used to automatically select the task list to which the current task belongs.
     *
     * @param currentTaskListId task list identifier
     */
    internal fun loadTaskLists(currentTaskListId: String) {
        tasksHelper.getTaskLists(realm)
                .map<Pair<RealmResults<TaskList>, Int>> { taskLists ->
                    // Find the index of the current task list
                    taskLists.forEachIndexed { i, taskList ->
                        if (taskList.id == currentTaskListId) {
                            return@map Pair(taskLists, i)
                        }
                    }

                    // Index not found, select the first task list
                    Pair(taskLists, 0)
                }
                .subscribe({ taskListsIndexPair ->
                    view()?.onTaskListsLoaded(taskListsIndexPair.first!!, taskListsIndexPair.second!!)
                }, { throwable ->
                    Timber.e(throwable.toString())
                    view()?.onTaskLoadError()
                })
    }

    // TODO: 2016-08-19 implement this using Firebase
    internal fun setDueTime(date: Date) {
        if (dueDate == null) {
            dueDate = date
        } else {
            Timber.d("old date %s", dueDate!!.toString())
            val oldCal = Calendar.getInstance()
            oldCal.time = dueDate

            val newCal = Calendar.getInstance()
            newCal.time = date

            oldCal.set(Calendar.HOUR_OF_DAY, newCal.get(Calendar.HOUR_OF_DAY))
            oldCal.set(Calendar.MINUTE, newCal.get(Calendar.MINUTE))
            dueDate = oldCal.time
            Timber.d("new date %s", dueDate!!.toString())
        }
    }

    /**
     * Set the reminder time. This requires that the due date is already set.
     *
     * @param date the reminder time
     */
    internal fun setReminderTime(date: Date) {
        dueDate?.let { dueDate ->
            // Get the year, month, and day in the user's timezone
            val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
            utcCal.time = dueDate
            val year = utcCal.get(Calendar.YEAR)
            val month = utcCal.get(Calendar.MONTH)
            val day = utcCal.get(Calendar.DAY_OF_MONTH)

            val cal = Calendar.getInstance()
            // Set the reminder time
            cal.time = date
            // Correct the date
            cal.set(year, month, day)

            reminder = cal.time
        }
    }

    internal fun removeReminder() {
        reminder = null
    }

    internal fun hasReminder(): Boolean = reminder != null

    /**
     * Set the title to be saved when the task is modified or a new one is created.
     *
     * @param taskTitle task title
     */
    internal fun setTaskTitle(taskTitle: String) {
        editTaskFields.title = taskTitle.trim { it <= ' ' }
    }

    /**
     * Set the notes to be saved when the task is modified or a new one is created.
     *
     * @param taskNotes task notes
     */
    internal fun setTaskNotes(taskNotes: String) {
        editTaskFields.notes = taskNotes.trim { it <= ' ' }
    }

    /**
     * Create a new task in the specified task list and include the fields inserted by the user.
     * The task is first created locally and then synced online if there is an active network connection.
     * If that's not the case, the task will be synced on the next refresh.
     *
     * @param taskListId task list identifier
     */
    internal fun newTask(taskListId: String) {
        // Nothing entered
        if (editTaskFields.isEmpty()) {
            view()?.onTaskSaved()
            return
        }
        // Create the Task offline
        val taskId = UUID.randomUUID().toString()
        val task = Task(taskId, taskListId)
        task.update(editTaskFields)
        task.synced = false
        task.reminder = reminder

        // Schedule the notification
        reminder?.let {
            task.assignNotificationId()
            notificationHelper.scheduleTaskNotification(task)
        }

        // Save the task locally
        realm.executeTransaction { it.copyToRealm(task) }

        // Update the widget
        widgetHelper.updateWidgets(taskListId)

        // Schedule a job that saves this task on the server
        CreateTaskJob.schedule(taskId, taskListId, editTaskFields)

        view()?.onTaskSaved()
    }

    /**
     * Modify the specified task using the fields changed by the user. The task is
     * first updated locally and then, if an active network connection is available,
     * it is updated on the server.
     *
     * @param taskListId task list identifier
     * @param taskId     task identifier
     * @param isOnline   `true` if there is an active network connection
     */
    internal fun updateTask(taskListId: String, taskId: String, isOnline: Boolean) {
        // No changes, return
        if (editTaskFields.isEmpty()) {
            view()?.onTaskSaved()
            return
        }
        // Update the task locally TODO: 2016-10-22 check this
        val managedTask = tasksHelper.getTask(taskId, realm) ?: return
        val reminderId = managedTask.reminder?.hashCode() ?: 0
        val notificationId = managedTask.notificationId
        realm.executeTransaction {
            managedTask.update(editTaskFields)
            managedTask.reminder = reminder
            managedTask.synced = false
        }

        widgetHelper.updateWidgets(taskListId)

        // Schedule a reminder only if there is one or it has changed
        if (managedTask.reminder?.hashCode() != reminderId)
            notificationHelper.scheduleTaskNotification(managedTask)

        // Cancel the notification if the user has removed the reminder
        if (reminderId != 0 && managedTask.reminder == null)
            notificationHelper.cancelTaskNotification(notificationId)

        // Update or clear the reminder
        val tasksDatabase = FirebaseDatabase.getInstance().getTasksDatabase()
        tasksDatabase.saveReminder(managedTask.id, reminder?.time)

        // Update the task on an active network connection
        if (isOnline) {
            tasksHelper.updateTask(taskListId, taskId, editTaskFields)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({ task ->
                        realm.executeTransaction { realm ->
                            realm.insertOrUpdate(task)
                            managedTask.synced = true
                        }
                        widgetHelper.updateWidgets(taskListId)
                    }, { throwable ->
                        Timber.e(throwable.toString())
                        view()?.onTaskSaveError()
                    })
        }
        view()?.onTaskSaved()
    }

    /**
     * Check if the due date is set.
     *
     * @return `true` if the due date is set, `false` otherwise
     */
    internal fun hasDueDate(): Boolean = dueDate != null

    /**
     * Remove the due date.
     * The reminder date cannot exist without it so it is removed as well.
     */
    internal fun removeDueDate() {
        dueDate = null
        reminder = null
    }

    override fun bindView(view: EditTaskView) {
        super.bindView(view)
        realm = Realm.getDefaultInstance()
    }

    override fun unbindView(view: EditTaskView) {
        super.unbindView(view)
        realm.close()
    }

    /**
     * Update the task with the specified fields.
     * Requires to executed in a Realm transaction.
     *
     * @param taskFields fields to be updated
     */
    fun Task.update(taskFields: TaskFields) {
        title = taskFields.title
        notes = taskFields.notes
        due = taskFields.dueDate?.let { utcDateFormat.parse(it) }
    }
}
