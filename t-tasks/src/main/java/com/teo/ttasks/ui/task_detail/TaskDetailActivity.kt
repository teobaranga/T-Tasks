package com.teo.ttasks.ui.task_detail

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.transition.Transition
import android.view.MenuItem
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.OvershootInterpolator
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.databinding.DataBindingUtil
import com.teo.ttasks.BuildConfig
import com.teo.ttasks.R
import com.teo.ttasks.data.model.Task
import com.teo.ttasks.data.model.TaskList
import com.teo.ttasks.databinding.ActivityTaskDetailBinding
import com.teo.ttasks.ui.activities.edit_task.EditTaskActivity
import com.teo.ttasks.util.AnimUtils
import org.koin.android.scope.currentScope
import org.threeten.bp.format.DateTimeFormatter

class TaskDetailActivity : AppCompatActivity(), TaskDetailView {

    private val taskDetailPresenter: TaskDetailPresenter by currentScope.inject()

    private val taskId by lazy(LazyThreadSafetyMode.NONE) {
        intent?.getStringExtra(EXTRA_TASK_ID)
    }

    private val taskListId by lazy(LazyThreadSafetyMode.NONE) {
        intent?.getStringExtra(EXTRA_TASK_LIST_ID)
    }

    internal lateinit var taskDetailBinding: ActivityTaskDetailBinding

    private lateinit var shareIntent: Intent

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        taskDetailBinding = DataBindingUtil.setContentView(this, R.layout.activity_task_detail)
        setSupportActionBar(taskDetailBinding.toolbar)

        taskDetailPresenter.bindView(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.enterTransition.addListener(object : AnimUtils.TransitionListener() {
                override fun onTransitionEnd(transition: Transition) {
                    ViewCompat.animate(taskDetailBinding.fab)
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .alpha(1.0f)
                        .setInterpolator(OvershootInterpolator())
                        .withLayer().start()
                }
            })
        }

        val action = intent.action
        if (savedInstanceState != null || action == ACTION_SKIP_ANIMATION) {
            // Skip the animation if requested or if the activity is getting recreated
            skipEnterAnimation()
        } else {
            enterAnimation()
        }

        // Add a context menu to the task header
        registerForContextMenu(taskDetailBinding.taskHeader)

        // Get the task
        taskId?.let {
            taskDetailPresenter.getTask(it)
        }

        taskListId?.let {
            taskDetailPresenter.getTaskList(it)
        }
    }

    override fun onTaskLoaded(task: Task) {
        taskDetailBinding.task = task

        // Create the share Intent for this task
        var extraText: String = task.title!!
        task.dueDate?.let { extraText += String.format(getString(R.string.extra_due_date), it.format(DateTimeFormatter.ISO_ZONED_DATE_TIME)) }
        if (task.hasNotes) extraText += String.format(getString(R.string.extra_notes), task.notes)

        shareIntent = Intent()
        shareIntent.action = Intent.ACTION_SEND
        shareIntent.putExtra(Intent.EXTRA_TEXT, extraText)
        shareIntent.type = "text/plain"
    }

    override fun onTaskLoadError() {
        Toast.makeText(this, R.string.error_task_loading, Toast.LENGTH_SHORT).show()
        finish()
    }

    override fun onTaskListLoaded(taskList: TaskList) {
        taskDetailBinding.taskList = taskList
    }

    override fun onTaskListLoadError() {
        // TODO: 2016-07-24 implement
    }

    override fun onTaskUpdated(task: Task) = onBackPressed()

    override fun onTaskDeleted() {
        Toast.makeText(this, R.string.task_deleted, Toast.LENGTH_SHORT).show()
        onBackPressed()
    }

    @Suppress("UNUSED_PARAMETER")
    fun onFabClicked(v: View) = taskDetailPresenter.updateCompletionStatus()

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.delete -> {
                val dialogClickListener = DialogInterface.OnClickListener { _, choice ->
                    when (choice) {
                        DialogInterface.BUTTON_POSITIVE -> taskDetailPresenter.deleteTask()
                        DialogInterface.BUTTON_NEGATIVE -> {
                        }
                    }
                }
                AlertDialog.Builder(this)
                    .setMessage("Delete this task?")
                    .setPositiveButton(android.R.string.yes, dialogClickListener)
                    .setNegativeButton(android.R.string.no, dialogClickListener)
                    .show()
            }
            R.id.edit -> {
                (taskId to taskListId).let { (taskId, taskListId) ->
                    if (taskId != null && taskListId != null) {
                        EditTaskActivity.startEdit(this, taskId, taskListId, null)
                    }
                }
            }
            else -> {
                super.onOptionsItemSelected(item)
            }
        }
        return true
    }

    private fun enterAnimation() {
        val fadeInAnimation = AlphaAnimation(0.0f, 1.0f)
        fadeInAnimation.duration = 200
        fadeInAnimation.startOffset = 100
        taskDetailBinding.taskTitle.startAnimation(fadeInAnimation)
        taskDetailBinding.taskListTitle.startAnimation(fadeInAnimation)
    }

    /**
     * Skip the enter animation.<br></br>
     * Used on versions below Lollipop because the lack of content transitions
     * makes the animations look like the app is slow.<br></br>
     * Also used when starting this activity from the widget, again because
     * there are no content transitions.
     */
    private fun skipEnterAnimation() {
        taskDetailBinding.fab.scaleX = 1f
        taskDetailBinding.fab.scaleY = 1f
        taskDetailBinding.fab.alpha = 1f
    }

    override fun onBackPressed() {
        // Remove the transition name so that the reverse shared transition doesn't play
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            taskDetailBinding.taskHeader.transitionName = null
        }
        super.onBackPressed()
    }

    override fun onDestroy() {
        super.onDestroy()
        taskDetailPresenter.unbindView(this)
    }

    companion object {

        const val EXTRA_TASK_ID = "taskId"
        const val EXTRA_TASK_LIST_ID = "taskListId"

        private const val ACTION_SKIP_ANIMATION = BuildConfig.APPLICATION_ID + ".SKIP_ANIMATION"

        /**
         * Get the Intent template used to start this activity from outside the application.
         * This is part of the process used when starting this activity from the widget.
         *
         * @param context    context
         * @param taskListId task list identifier
         * @return an incomplete Intent that needs to be completed by adding the extra
         * [EXTRA_TASK_ID] before being used to start this activity
         */
        fun getIntentTemplate(context: Context, taskListId: String): Intent {
            return Intent(context, TaskDetailActivity::class.java).apply {
                putExtra(EXTRA_TASK_LIST_ID, taskListId)
                action = ACTION_SKIP_ANIMATION
            }
        }

        fun getStartIntent(context: Context, taskId: String, taskListId: String, skipAnimation: Boolean = false): Intent {
            return Intent(context, this::class.java.declaringClass).apply {
                putExtra(EXTRA_TASK_ID, taskId)
                putExtra(EXTRA_TASK_LIST_ID, taskListId)
                if (skipAnimation) {
                    action = ACTION_SKIP_ANIMATION
                }
            }
        }
    }
}
