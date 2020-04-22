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
import coil.Coil
import coil.request.LoadRequest
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.mikepenz.aboutlibraries.LibsBuilder
import com.teo.ttasks.R
import com.teo.ttasks.data.TaskListsAdapter
import com.teo.ttasks.data.model.TaskList
import com.teo.ttasks.databinding.ActivityMainBinding
import com.teo.ttasks.ui.activities.BaseActivity
import com.teo.ttasks.ui.activities.SettingsActivity
import com.teo.ttasks.ui.activities.edit_task.EditTaskActivity
import com.teo.ttasks.ui.activities.sign_in.SignInActivity.Companion.startSignInActivity
import com.teo.ttasks.ui.fragments.accounts.AccountInfoDialogFragment
import com.teo.ttasks.ui.fragments.task_lists.TaskListsFragment
import com.teo.ttasks.ui.fragments.tasks.TasksFragment
import com.teo.ttasks.ui.fragments.tasks.TasksViewModel
import com.teo.ttasks.util.getColorFromAttr
import org.koin.androidx.scope.lifecycleScope
import org.koin.androidx.viewmodel.scope.viewModel
import timber.log.Timber

// TODO: 2015-12-29 implement multiple accounts
class MainActivity : BaseActivity() {

    companion object {
        private const val TAG_TASKS = "tasks"

        // private const val RC_ADD = 4;

        fun Context.startMainActivity() = startActivity(Intent(this, MainActivity::class.java))
    }

    private var tasksFragment: TasksFragment? = null

    private lateinit var mainBinding: ActivityMainBinding

    private lateinit var taskListsAdapter: ArrayAdapter<TaskList>

    private lateinit var accountMenuItem: MenuItem

    private val viewModel by lifecycleScope.viewModel<MainViewModel>(this)

    private val tasksViewModel by lazy {
        lifecycleScope.viewModel<TasksViewModel>(tasksFragment!!)
    }

    private var accountInfoDialogFragment: AccountInfoDialogFragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Reset the window background (after displaying the splash screen)
        window.setBackgroundDrawable(ColorDrawable(getColorFromAttr(R.attr.colorPrimary)))

        // Show the SignIn activity if there's no user connected
        if (!viewModel.isSignedIn()) {
            signOut()
            return
        }

        mainBinding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        mainBinding.lifecycleOwner = this
        mainBinding.viewModel = viewModel

        setupViews()

        setupObservers()

        if (savedInstanceState == null) {
            // Inflate the tasks fragment
            TasksFragment.newInstance().run {
                supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.fragment_container, this, TAG_TASKS)
                    .commit()
                tasksFragment = this
            }
        } else {
            tasksFragment = supportFragmentManager.findFragmentByTag(TAG_TASKS) as TasksFragment
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_tasks, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        accountMenuItem = menu?.findItem(R.id.menu_account)!!
        viewModel.profilePicture.value?.let {
            Coil.execute(LoadRequest.Builder(this)
                .data(it)
                .target { drawable ->
                    accountMenuItem.icon = drawable
                }
                .build())
        }
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_account -> {
                AccountInfoDialogFragment.newInstance().let {
                    it.show(supportFragmentManager, "account_info")
                    accountInfoDialogFragment = it
                }
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

    private fun setupViews() {

        mainBinding.taskLists.apply {

            itemLongClickListener = { position ->
                val taskListId = (getItemAtPosition(position) as TaskList).id
                TaskListsFragment.newInstance(taskListId)
                    .show(supportFragmentManager, "taskList")
                true
            }

            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(adapterView: AdapterView<*>, view: View?, position: Int, id: Long) {
                    val taskListId = (adapterView.getItemAtPosition(position) as TaskList).id
                    viewModel.activeTaskListId = taskListId
                    tasksViewModel.value.taskListId = taskListId
                }

                override fun onNothingSelected(adapterView: AdapterView<*>) {}
            }
        }
    }

    private fun setupObservers() {

        viewModel.taskLists.observe(this, Observer { taskLists ->
            taskListsAdapter = TaskListsAdapter(supportActionBar!!.themedContext, taskLists)
            mainBinding.taskLists.adapter = taskListsAdapter
        })

        viewModel.activeTaskList.observe(this, Observer { activeTaskList ->
            if (activeTaskList != null) {
                val position = taskListsAdapter.getPosition(activeTaskList)
                mainBinding.taskLists.setSelection(position, false)
            }
        })

        viewModel.signedIn.observe(this, Observer { signedIn ->
            if (!signedIn) {
                signOut()
            }
        })

        viewModel.profilePicture.observe(this, Observer {
            invalidateOptionsMenu()
        })

        viewModel.events.observe(this, Observer {
            it.getIfUnhandled()?.let { action ->
                when (action) {
                    MainViewModel.ActionEvent.ADD_TASK -> {
                        EditTaskActivity.startCreate(this, viewModel.activeTaskListId!!, null)
                    }
                    MainViewModel.ActionEvent.ADD_TASK_LIST -> {
                        TaskListsFragment.newInstance().show(supportFragmentManager, "taskList")
                    }
                    MainViewModel.ActionEvent.ABOUT -> {
                        LibsBuilder()
                            .withActivityTitle(getString(R.string.about))
                            .start(this)
                    }
                    MainViewModel.ActionEvent.SETTINGS -> {
                        SettingsActivity.start(this)
                    }
                }
            }
            accountInfoDialogFragment?.dismiss()
            accountInfoDialogFragment = null
        })
    }

    private fun signOut() {
        // Return to the sign in activity
        startSignInActivity(true)
        finish()
    }
}
