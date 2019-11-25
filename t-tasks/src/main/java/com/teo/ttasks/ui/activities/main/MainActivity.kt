package com.teo.ttasks.ui.activities.main

import android.content.Context
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.core.view.updateLayoutParams
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
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
import com.teo.ttasks.ui.fragments.accounts.AccountInfoDialogFragment
import com.teo.ttasks.ui.fragments.accounts.AccountInfoDialogFragment.AccountInfoListener
import com.teo.ttasks.ui.fragments.tasks.TasksFragment
import com.teo.ttasks.util.getColorFromAttr
import org.koin.android.scope.currentScope
import timber.log.Timber

// TODO: 2015-12-29 implement multiple accounts
open class MainActivity : BaseActivity(), AccountInfoListener {

    companion object {
        private const val TAG_TASKS = "tasks"

        // private const val RC_ADD = 4;

        fun Context.startMainActivity() = startActivity(Intent(this, MainActivity::class.java))
    }

    private val mainActivityPresenter: MainActivityPresenter by currentScope.inject()

    private var tasksFragment: TasksFragment? = null

    private lateinit var mainBinding: ActivityMainBinding

    private lateinit var taskListsAdapter: ArrayAdapter<TaskList>

    private lateinit var accountMenuItem: MenuItem

    private lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Reset the window background (after displaying the splash screen)
        window.setBackgroundDrawable(ColorDrawable(getColorFromAttr(R.attr.colorPrimary)))

        viewModel = ViewModelProvider(this)[MainViewModel::class.java]

        // Show the SignIn activity if there's no user connected
        if (!viewModel.isSignedIn()) {
            signOut()
            return
        }

        mainBinding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        taskListsAdapter = TaskListsAdapter(supportActionBar!!.themedContext)

        mainBinding.spinnerTaskLists.apply {
            adapter = taskListsAdapter

            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(adapterView: AdapterView<*>, view: View?, position: Int, id: Long) {
                    val taskListId = (adapterView.getItemAtPosition(position) as TaskList).id
                    viewModel.activeTaskListId = taskListId
                    tasksFragment?.updateTaskListId(taskListId)
                }

                override fun onNothingSelected(adapterView: AdapterView<*>) {}
            }
        }

        viewModel.taskLists.observe(this, Observer {  taskLists ->
            taskListsAdapter = TaskListsAdapter(supportActionBar!!.themedContext, taskLists)
            mainBinding.spinnerTaskLists.adapter = taskListsAdapter
        })

        viewModel.activeTaskList.observe(this, Observer {  activeTaskList ->
            if (activeTaskList != null) {
                val position = taskListsAdapter.getPosition(activeTaskList)
                mainBinding.spinnerTaskLists.setSelection(position, false)
            }
        })

        viewModel.signedIn.observe(this, Observer { signedIn ->
            if (!signedIn) {
                signOut()
            }
        })

        mainBinding.fab.setOnClickListener {
            EditTaskActivity.startCreate(this, viewModel.activeTaskListId!!, null)
        }

        // Only set the active selection or active profile if we do not recreate the activity
        if (savedInstanceState == null) {
            // Inflate the tasks fragment
            tasksFragment = tasksFragment
                ?: (supportFragmentManager.findFragmentByTag(TAG_TASKS) as? TasksFragment
                    ?: TasksFragment.newInstance(viewModel.activeTaskListId!!))

            supportFragmentManager
                .beginTransaction()
                .replace(R.id.fragment_container, tasksFragment!!, TAG_TASKS)
                .commit()
        }
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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId) {
            R.id.menu_account -> {
                val accountDialogFragment = AccountInfoDialogFragment.newInstance()
                accountDialogFragment.show(supportFragmentManager, "account_info")
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    fun onTaskListsLoadError() {
        // TODO: 2016-07-24 implement
    }

    fun fab(): FloatingActionButton = mainBinding.fab

    /**
     * Toggle the scroll behavior of the AppBar.
     *
     * @param enable if `true`, enables the scrolling system, which moves the toolbar out of the way when scrolling
     * down and brings it back when scrolling up. If false, disables the whole scrolling system, which pins it in place.
     * This is used when the content of the fragment is not big enough to require scrolling, such is the case when a
     * short list or an empty view is displayed.
     */
    fun setAppBarScrolling(enable: Boolean) {
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
        Timber.v("Scroll enabled: $enable")
    }

    override fun onSignOut() {
        mainActivityPresenter.signOut()
    }

    override fun onSettingsShow() {
        SettingsActivity.start(this)
    }

    override fun onAboutShow() {
        AboutActivity.start(this)
    }

    private fun signOut() {
        // Return to the sign in activity
        startSignInActivity(true)
        finish()
    }
}
