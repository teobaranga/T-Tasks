package com.teo.ttasks.ui.activities.task_detail

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.databinding.DataBindingUtil
import android.os.Build
import android.os.Bundle
import android.support.v4.view.MenuItemCompat
import android.support.v4.view.ViewCompat
import android.support.v7.app.AlertDialog
import android.support.v7.widget.PopupMenu
import android.support.v7.widget.ShareActionProvider
import android.transition.Transition
import android.view.Gravity
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.OvershootInterpolator
import android.widget.Toast
import com.teo.ttasks.BuildConfig
import com.teo.ttasks.R
import com.teo.ttasks.data.model.TTask
import com.teo.ttasks.data.model.TTaskList
import com.teo.ttasks.databinding.ActivityTaskDetailBinding
import com.teo.ttasks.ui.activities.edit_task.EditTaskActivity
import com.teo.ttasks.util.AnimUtils
import com.teo.ttasks.util.DateUtils
import dagger.android.support.DaggerAppCompatActivity
import javax.inject.Inject

class TaskDetailActivity : DaggerAppCompatActivity(), TaskDetailView {

    companion object {

        const val EXTRA_TASK_ID = "taskId"
        const val EXTRA_TASK_LIST_ID = "taskListId"

        private const val ACTION_SKIP_ANIMATION = BuildConfig.APPLICATION_ID + "SKIP_ANIMATION"

        fun start(context: Context, taskId: String, taskListId: String, bundle: Bundle?) {
            val starter = getStartIntent(context, taskId, taskListId)
            context.startActivity(starter, bundle)
        }

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
            val starter = Intent(context, TaskDetailActivity::class.java)
            starter.putExtra(EXTRA_TASK_LIST_ID, taskListId)
            starter.action = ACTION_SKIP_ANIMATION
            return starter
        }

        fun getStartIntent(context: Context, taskId: String, taskListId: String, skipAnimation: Boolean = false): Intent {
            val starter = Intent(context, TaskDetailActivity::class.java)
            starter.putExtra(EXTRA_TASK_ID, taskId)
            starter.putExtra(EXTRA_TASK_LIST_ID, taskListId)
            if (skipAnimation) {
                starter.action = ACTION_SKIP_ANIMATION
            }
            return starter
        }
    }

    @Inject internal lateinit var taskDetailPresenter: TaskDetailPresenter

    internal lateinit var taskDetailBinding: ActivityTaskDetailBinding

    private lateinit var taskId: String
    private lateinit var taskListId: String

    private lateinit var shareIntent: Intent

    private val overflowClickListener = View.OnClickListener {
        val popup = PopupMenu(this, taskDetailBinding.bar)
        popup.gravity = Gravity.END
        popup.inflate(R.menu.menu_task_detail)
        (MenuItemCompat.getActionProvider(popup.menu.findItem(R.id.share)) as ShareActionProvider).setShareIntent(shareIntent)
        popup.setOnMenuItemClickListener { item ->
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
            }
            true
        }
        popup.show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        taskDetailBinding = DataBindingUtil.setContentView(this, R.layout.activity_task_detail)
        taskDetailPresenter.bindView(this)

        taskDetailBinding.more.setOnClickListener(overflowClickListener)

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
        taskId = intent.getStringExtra(EXTRA_TASK_ID)
        taskListId = intent.getStringExtra(EXTRA_TASK_LIST_ID)
        taskDetailPresenter.getTask(taskId)
        taskDetailPresenter.getTaskList(taskListId)
    }

    override fun onTaskLoaded(task: TTask) {
        taskDetailBinding.task = task

        // Create the share Intent for this task
        var extraText: String = task.title
        task.due?.let { extraText += String.format(getString(R.string.extra_due_date), DateUtils.formatDate(this, it)) }
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

    override fun onTaskListLoaded(taskList: TTaskList) {
        taskDetailBinding.taskList = taskList
    }

    override fun onTaskListLoadError() {
        // TODO: 2016-07-24 implement
    }

    override fun onTaskUpdated(task: TTask) = onBackPressed()

    override fun onTaskDeleted() {
        Toast.makeText(this, R.string.task_deleted, Toast.LENGTH_SHORT).show()
        onBackPressed()
    }

    @Suppress("UNUSED_PARAMETER")
    fun onFabClicked(v: View) = taskDetailPresenter.updateCompletionStatus()

    @Suppress("UNUSED_PARAMETER")
    fun onBackClicked(v: View) = onBackPressed()

    @Suppress("UNUSED_PARAMETER")
    fun onEditClicked(v: View) = EditTaskActivity.startEdit(this, taskId, taskListId, null)

    private fun enterAnimation() {
        val fadeInAnimation = AlphaAnimation(0.0f, 1.0f)
        fadeInAnimation.duration = 200
        fadeInAnimation.startOffset = 100
        taskDetailBinding.back.startAnimation(fadeInAnimation)
        taskDetailBinding.edit.startAnimation(fadeInAnimation)
        taskDetailBinding.more.startAnimation(fadeInAnimation)
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
}
