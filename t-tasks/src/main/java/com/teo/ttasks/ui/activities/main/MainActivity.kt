package com.teo.ttasks.ui.activities.main

import android.content.Context
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import androidx.core.view.updateLayoutParams
import androidx.databinding.DataBindingUtil
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.teo.ttasks.R
import com.teo.ttasks.data.TaskListsAdapter
import com.teo.ttasks.data.model.TaskList
import com.teo.ttasks.databinding.ActivityMainBinding
import com.teo.ttasks.ui.activities.AboutActivity
import com.teo.ttasks.ui.activities.BaseActivity
import com.teo.ttasks.ui.activities.SettingsActivity
import com.teo.ttasks.ui.activities.edit_task.EditTaskActivity
import com.teo.ttasks.ui.activities.sign_in.SignInActivity.Companion.startSignInActivity
import com.teo.ttasks.ui.fragments.AccountInfoDialogFragment
import com.teo.ttasks.ui.fragments.AccountInfoDialogFragment.AccountInfoListener
import com.teo.ttasks.ui.fragments.tasks.TasksFragment
import com.teo.ttasks.util.getColorFromAttr
import org.koin.android.scope.currentScope
import timber.log.Timber

// TODO: 2015-12-29 implement multiple accounts
open class MainActivity : BaseActivity(), MainView, AccountInfoListener {

    companion object {
        private const val TAG_TASKS = "tasks"

        // private const val RC_ADD = 4;
        private const val RC_NIGHT_MODE = 415

        fun Context.startMainActivity() = startActivity(Intent(this, MainActivity::class.java))
    }

    private val mainActivityPresenter: MainActivityPresenter by currentScope.inject()

    private var tasksFragment: TasksFragment? = null

    private lateinit var mainBinding: ActivityMainBinding

    private lateinit var taskListsAdapter: TaskListsAdapter

    private lateinit var accountMenuItem: MenuItem

    private lateinit var lastAccessedTaskId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setBackgroundDrawable(ColorDrawable(getColorFromAttr(R.attr.colorPrimary)))
        mainActivityPresenter.bindView(this)

        // Show the SignIn activity if there's no user connected
        if (!mainActivityPresenter.isSignedIn()) {
            onSignedOut()
            return
        }

        mainBinding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        val (taskLists, index) = mainActivityPresenter.getTaskLists()
        lastAccessedTaskId = taskLists[index].id

        taskListsAdapter = TaskListsAdapter(supportActionBar!!.themedContext, taskLists)

        mainBinding.spinnerTaskLists.apply {
            adapter = taskListsAdapter

            setSelection(index, false)

            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(adapterView: AdapterView<*>, view: View?, position: Int, id: Long) {
                    val taskListId = (adapterView.getItemAtPosition(position) as TaskList).id
                    lastAccessedTaskId = taskListId
                    tasksFragment?.updateTaskListId(taskListId)
                }

                override fun onNothingSelected(adapterView: AdapterView<*>) {}
            }
        }

        mainBinding.fab.setOnClickListener {
            EditTaskActivity.startCreate(this, lastAccessedTaskId, null)
        }

        // Only set the active selection or active profile if we do not recreate the activity
        if (savedInstanceState == null) {
            // Inflate the tasks fragment
            tasksFragment = tasksFragment
                ?: (supportFragmentManager.findFragmentByTag(TAG_TASKS) as? TasksFragment
                    ?: TasksFragment.newInstance(lastAccessedTaskId))

            supportFragmentManager
                .beginTransaction()
                .replace(R.id.fragment_container, tasksFragment!!, TAG_TASKS)
                .commit()
        }
//        mainActivityPresenter.subscribeToTaskLists()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_tasks, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        accountMenuItem = menu?.findItem(R.id.menu_account)!!
        mainActivityPresenter.loadProfilePicture(accountMenuItem)
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return when(item?.itemId) {
            R.id.menu_account -> {
                val accountDialogFragment = AccountInfoDialogFragment.newInstance()
                accountDialogFragment.show(supportFragmentManager, "account_info")
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onStop() {
        super.onStop()
        mainActivityPresenter.lastAccessedTaskListId = lastAccessedTaskId
    }

    override fun onDestroy() {
        mainActivityPresenter.unbindView(this)
        super.onDestroy()
    }

    override fun onUserCover(coverUrl: String) {
    }

    /**
     * Load the task lists and prepare them to be displayed.
     * Select the last accessed task list.
     */
    override fun onTaskListsLoaded(taskLists: List<TaskList>, currentTaskListIndex: Int) {
        // Set the task list only if it's different than the currently selected one
        if (mainBinding.spinnerTaskLists.selectedItemPosition != currentTaskListIndex) {
            // TODO: use DiffUtil?
            taskListsAdapter.clear()
            taskListsAdapter.addAll(taskLists)
            // Restore previously selected task list
            mainBinding.spinnerTaskLists.setSelection(currentTaskListIndex)
        }
    }

    override fun onTaskListsLoadError() {
        // TODO: 2016-07-24 implement
    }

    override fun onSignedOut() {
        // Return to the sign in activity
        startSignInActivity(true)
        finish()
    }

    fun fab(): FloatingActionButton = mainBinding.fab

    /**
     * Set the scroll behavior of the FAB.
     *
     * @param enable if true, enables the scrolling system, which moves the toolbar and the FAB
     * out of the way when scrolling down and brings them back when scrolling up. If false, disables the whole
     * scrolling system, which pins the toolbar and the FAB in place. This is used when the content of the fragment
     * is not big enough to require scrolling, such is the case when a short list or an empty view is displayed.
     */
    fun setFabScrolling(enable:Boolean, delay: Boolean = false) {
        val runnable = Runnable {
            mainBinding.toolbar.updateLayoutParams<AppBarLayout.LayoutParams> {
                val flags = if (enable) {
                    AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL or AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS
                } else {
                    0
                }
                if (scrollFlags != flags) {
                    scrollFlags = flags
                }
            }
        }
        if (enable) {
            runnable.run()
        } else {
            mainBinding.fab.postDelayed(runnable, if (delay) 300L else 0L)
        }
    }

    override fun onSignOut() {
        mainActivityPresenter.signOut()
    }

    override fun onSettingsShow() {
        SettingsActivity.startForResult(this, RC_NIGHT_MODE)
    }

    override fun onAboutShow() {
        AboutActivity.start(this)
    }
}
