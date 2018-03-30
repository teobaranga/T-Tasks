package com.teo.ttasks.ui.activities.main

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.databinding.DataBindingUtil
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.support.design.widget.AppBarLayout
import android.support.design.widget.FloatingActionButton
import android.support.v4.app.Fragment
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.AdapterView
import com.androidhuman.rxfirebase2.auth.rxGetCurrentUser
import com.androidhuman.rxfirebase2.auth.rxReload
import com.google.firebase.auth.FirebaseAuth
import com.mikepenz.google_material_typeface_library.GoogleMaterial
import com.mikepenz.materialdrawer.AccountHeader
import com.mikepenz.materialdrawer.AccountHeaderBuilder
import com.mikepenz.materialdrawer.Drawer
import com.mikepenz.materialdrawer.DrawerBuilder
import com.mikepenz.materialdrawer.holder.ImageHolder
import com.mikepenz.materialdrawer.model.DividerDrawerItem
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem
import com.mikepenz.materialdrawer.model.ProfileDrawerItem
import com.mikepenz.materialdrawer.model.SecondaryDrawerItem
import com.mikepenz.materialdrawer.model.interfaces.IProfile
import com.mikepenz.materialdrawer.util.DrawerUIUtils
import com.squareup.picasso.Picasso
import com.squareup.picasso.Target
import com.teo.ttasks.R
import com.teo.ttasks.data.TaskListsAdapter
import com.teo.ttasks.data.model.TTaskList
import com.teo.ttasks.databinding.ActivityMainBinding
import com.teo.ttasks.receivers.NetworkInfoReceiver
import com.teo.ttasks.ui.activities.AboutActivity
import com.teo.ttasks.ui.activities.BaseActivity
import com.teo.ttasks.ui.activities.SettingsActivity
import com.teo.ttasks.ui.activities.sign_in.SignInActivity
import com.teo.ttasks.ui.fragments.task_lists.TaskListsFragment
import com.teo.ttasks.ui.fragments.tasks.TasksFragment
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import java.lang.Exception
import javax.inject.Inject

// TODO: 2015-12-29 implement multiple accounts
open class MainActivity : BaseActivity(), MainView {

    companion object {
        private const val TAG_TASKS = "tasks"
        private const val TAG_TASK_LISTS = "taskLists"

        // private const val RC_ADD = 4;
        private const val RC_NIGHT_MODE = 415

        private const val ID_TASKS: Long = 0x01
        private const val ID_TASK_LISTS: Long = 0x02
        //        private const val ID_ADD_ACCOUNT = 0x01
//        private const val ID_MANAGE_ACCOUNT = 0x02
        private const val ID_SETTINGS: Long = 0xF0
        private const val ID_HELP_AND_FEEDBACK: Long = 0xF1
        private const val ID_ABOUT: Long = 0xF2
        private const val ID_SIGN_OUT: Long = 0xF3

        fun start(context: Context) {
            val starter = Intent(context, MainActivity::class.java)
            context.startActivity(starter)
        }
    }

    @Inject internal lateinit var mainActivityPresenter: MainActivityPresenter
    @Inject internal lateinit var networkInfoReceiver: NetworkInfoReceiver

    internal lateinit var profile: ProfileDrawerItem
    internal lateinit var accountHeader: AccountHeader
    internal lateinit var tasksFragment: TasksFragment
    internal lateinit var taskListsFragment: TaskListsFragment

    private lateinit var mainBinding: ActivityMainBinding

    private lateinit var taskListsAdapter: TaskListsAdapter

    private lateinit var firebaseAuth: FirebaseAuth
    private var authStateListener: FirebaseAuth.AuthStateListener? = null

    /** The profile of the currently logged in user  */
    private lateinit var drawer: Drawer

    private var recreate: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainActivityPresenter.bindView(this)

        // Show the SignIn activity if there's no user connected
        firebaseAuth = FirebaseAuth.getInstance()

        val currentUser = firebaseAuth.currentUser
        if (currentUser == null) {
            SignInActivity.start(this, false)
            finish()
            return
        } else {
            // TODO refresh the user picture only if necessary
            currentUser.rxReload()
                    .subscribeOn(Schedulers.io())
                    .andThen(firebaseAuth.rxGetCurrentUser())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            { firebaseUser ->
                                onUserPicture(firebaseUser.photoUrl.toString())
                            })
        }

        firebaseAuth.addAuthStateListener {
            authStateListener = FirebaseAuth.AuthStateListener {
                firebaseAuth ->
                if (firebaseAuth.currentUser == null) {
                    // TODO Unregister all task listeners
                    mainActivityPresenter.clearUser()
                    SignInActivity.start(this, true)
                    finish()
                }
            }
        }

        mainBinding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        if (savedInstanceState == null) {
            // Create the tasks fragment
            tasksFragment = TasksFragment.newInstance()
            taskListsFragment = TaskListsFragment.newInstance()
        } else {
            // Get the tasks fragment
            val fragment = supportFragmentManager.findFragmentByTag(TAG_TASKS)
            tasksFragment = fragment as? TasksFragment ?: TasksFragment.newInstance()
            // Get the task lists fragment
            val fragmentTaskLists = supportFragmentManager.findFragmentByTag(TAG_TASK_LISTS)
            taskListsFragment = fragmentTaskLists as? TaskListsFragment ?: TaskListsFragment.newInstance()
        }

        taskListsAdapter = TaskListsAdapter(supportActionBar!!.themedContext)
        mainBinding.spinnerTaskLists.adapter = taskListsAdapter
        mainBinding.spinnerTaskLists.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(adapterView: AdapterView<*>, view: View, position: Int, id: Long) {
                val taskList = adapterView.getItemAtPosition(position) as TTaskList
                val taskListId = taskList.id
                mainActivityPresenter.setLastAccessedTaskList(taskListId)
                tasksFragment.updateTaskListId(taskListId)
            }

            override fun onNothingSelected(adapterView: AdapterView<*>) {}
        }

        profile = ProfileDrawerItem()
                .withName(mainActivityPresenter.userName)
                .withEmail(mainActivityPresenter.userEmail)
                .withNameShown(true)
                .withIdentifier(0)
                .withTag(ProfileIconTarget())

        // Create the AccountHeader
        accountHeader = AccountHeaderBuilder()
                .withActivity(this)
                .withHeaderBackground(R.color.colorPrimary)
                .withCurrentProfileHiddenInList(true)
                .withSelectionListEnabledForSingleProfile(false)
                .addProfiles(
                        profile
//                        ProfileSettingDrawerItem()
//                                .withName(resources.getString(R.string.drawer_add_account))
//                                .withIcon(GoogleMaterial.Icon.gmd_account_add)
//                                .withIdentifier(ID_ADD_ACCOUNT),
//                        ProfileSettingDrawerItem()
//                                .withName(resources.getString(R.string.drawer_manage_account))
//                                .withIcon(GoogleMaterial.Icon.gmd_settings)
//                                .withIdentifier(ID_MANAGE_ACCOUNT)
                )
                .withOnAccountHeaderListener { _: View, profile: IProfile<*>, _: Boolean ->
                    Timber.d(profile.toString())
                    true
                }
                .withSavedInstance(savedInstanceState)
                .build()

        // Create the drawer
        drawer = DrawerBuilder()
                .withActivity(this)
                .withToolbar(toolbar()!!)
                .withAccountHeader(accountHeader)
                .addDrawerItems(
                        PrimaryDrawerItem()
                                .withName(R.string.tasks)
                                .withIcon(R.drawable.ic_tasks_24dp)
                                .withIconTintingEnabled(true)
                                .withIdentifier(ID_TASKS),
                        PrimaryDrawerItem()
                                .withName(R.string.task_lists)
                                .withIcon(R.drawable.ic_task_lists_24dp)
                                .withIconTintingEnabled(true)
                                .withIdentifier(ID_TASK_LISTS),
                        DividerDrawerItem(),
                        SecondaryDrawerItem()
                                .withName(resources.getString(R.string.drawer_settings))
                                .withIcon(GoogleMaterial.Icon.gmd_settings)
                                .withIdentifier(ID_SETTINGS)
                                .withSelectable(false),
                        SecondaryDrawerItem()
                                .withName(R.string.drawer_help_and_feedback)
                                .withIcon(GoogleMaterial.Icon.gmd_help)
                                .withIdentifier(ID_HELP_AND_FEEDBACK)
                                .withSelectable(false),
                        SecondaryDrawerItem()
                                .withName(R.string.drawer_about)
                                .withIcon(GoogleMaterial.Icon.gmd_info_outline)
                                .withIdentifier(ID_ABOUT)
                                .withSelectable(false),
                        SecondaryDrawerItem()
                                .withName(R.string.sign_out)
                                .withIcon(R.drawable.ic_sign_out_24dp)
                                .withIconTintingEnabled(true)
                                .withIdentifier(ID_SIGN_OUT)
                                .withSelectable(false)
                )
                .withOnDrawerItemClickListener { _, _, drawerItem ->
                    // The header and footer items don't contain a drawerItem
                    drawerItem?.let {
                        val currentFragment: Fragment? = supportFragmentManager.findFragmentById(R.id.fragment_container)
                        var fragment: Fragment? = null
                        var tag: String? = null
                        val supportActionBar = supportActionBar!!
                        when (drawerItem.identifier) {
                            ID_TASKS -> {
                                if (currentFragment is TasksFragment)
                                    return@withOnDrawerItemClickListener false
                                supportActionBar.setDisplayShowTitleEnabled(false)
                                mainBinding.spinnerTaskLists.visibility = VISIBLE
                                fragment = tasksFragment
                                tag = TAG_TASKS
                            }
                            ID_TASK_LISTS -> {
                                if (currentFragment is TaskListsFragment)
                                    return@withOnDrawerItemClickListener false
                                supportActionBar.setTitle(R.string.task_lists)
                                supportActionBar.setDisplayShowTitleEnabled(true)
                                mainBinding.spinnerTaskLists.visibility = GONE
                                fragment = taskListsFragment
                                tag = TAG_TASK_LISTS
                            }
                            ID_SETTINGS -> SettingsActivity.startForResult(this, RC_NIGHT_MODE)
                            ID_ABOUT -> AboutActivity.start(this)
                            ID_SIGN_OUT -> firebaseAuth.signOut()
                            else -> {
                                // If we're being restored from a previous state,
                                // then we don't need to do anything and should return or else
                                // we could end up with overlapping fragments.
                            }
                        }
                        Timber.d("fragment is %s", fragment)
                        fragment?.let {
                            mainBinding.appbar.setExpanded(true)
                            currentFragment?.let {
                                Timber.v("Detaching current fragment %s", it)
                                supportFragmentManager.beginTransaction().detach(it).commit()
                            }
                            when (supportFragmentManager.findFragmentByTag(tag)) {
                                null -> {
                                    Timber.v("Adding new fragment with tag %s", tag)
                                    supportFragmentManager
                                            .beginTransaction()
                                            .add(R.id.fragment_container, fragment, tag)
                                            .commit()
                                }
                                else -> {
                                    Timber.v("Re-attaching fragment with tag %s", tag)
                                    supportFragmentManager.beginTransaction().attach(fragment).commit()
                                }
                            }
                        }
                    }
                    false
                }
                .withSavedInstance(savedInstanceState)
                .build()

        // Only set the active selection or active profile if we do not recreate the activity
        if (savedInstanceState == null) {
            // set the selection to the first item
            drawer.setSelectionAtPosition(1)
            //set the active profile
            accountHeader.activeProfile = profile
        } else {
            // Restore state
            when (drawer.currentSelection) {
                ID_TASKS -> {
                    supportActionBar!!.setDisplayShowTitleEnabled(false)
                    mainBinding.spinnerTaskLists.visibility = VISIBLE
                }
                ID_TASK_LISTS -> {
                    // Restore visibility of the task lists spinner & toolbar title
                    mainBinding.spinnerTaskLists.visibility = GONE

                    supportActionBar!!.setTitle(R.string.task_lists)
                    supportActionBar!!.setDisplayShowTitleEnabled(true)
                }
            }
        }

        mainActivityPresenter.getTaskLists()
        mainActivityPresenter.loadUserPictures()
    }

    override fun onPostResume() {
        super.onPostResume()
        if (recreate) {
            // Recreate the activity after a delay that is equal to the drawer close delay
            // otherwise, the drawer will open unexpectedly after a night mode change
            Handler().postDelayed({ this.recreate() }, 50)
            recreate = false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mainActivityPresenter.unbindView(this)
        authStateListener?.let { firebaseAuth.removeAuthStateListener(it) }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        var outStateVar = outState
        // add the values which need to be saved from the drawer to the bundle
        outStateVar = drawer.saveInstanceState(outStateVar)
        // add the values which need to be saved from the accountHeader to the bundle
        outStateVar = accountHeader.saveInstanceState(outStateVar)
        super.onSaveInstanceState(outStateVar)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            RC_NIGHT_MODE -> if (resultCode == Activity.RESULT_OK)
                recreate = true
        }//            case RC_ADD:
        //                if(resultCode == RESULT_OK) {
        //                    MaterialAccount acc = new MaterialAccount(this.getResources(), data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME),"", R.drawable.ic_photo, R.drawable.ic_cover);
        //                    this.addAccount(acc);
        //                    Toast.makeText(MainActivity.this, data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME), Toast.LENGTH_SHORT).show();
        //                }
        //                break;
    }

    override fun onApiReady() {
        if (networkInfoReceiver.isOnline(this)) {
            mainActivityPresenter.refreshTaskLists()
            mainActivityPresenter.loadCurrentUser()
        }
    }

    /**
     * Load the profile picture into the current profile
     * and display it in the Navigation drawer header
     */
    override fun onUserPicture(pictureUrl: String) {
        Picasso.get()
                .load(pictureUrl)
                .placeholder(DrawerUIUtils.getPlaceHolder(this))
                .into(profile.tag as Target)
    }

    override fun onUserCover(coverUrl: String) {
        accountHeader.setHeaderBackground(ImageHolder(coverUrl))
    }

    /**
     * Load the task lists and prepare them to be displayed
     * Select the last accessed task list
     */
    override fun onTaskListsLoaded(taskLists: List<TTaskList>, currentTaskListIndex: Int) {
        taskListsAdapter.clear()
        taskListsAdapter.addAll(taskLists)
        // Restore previously selected task list
        mainBinding.spinnerTaskLists.setSelection(currentTaskListIndex)
    }

    override fun onTaskListsLoadError() {
        // TODO: 2016-07-24 implement
    }

    override fun onBackPressed() {
        if (drawer.isDrawerOpen)
            drawer.closeDrawer()
        else
            super.onBackPressed()
    }

    /**
     * Enable the scrolling system, which moves the toolbar and the FAB
     * out of the way when scrolling down and brings them back when scrolling up.
     */
    fun enableScrolling() {
        val layoutParams = mainBinding.toolbar.layoutParams as AppBarLayout.LayoutParams
        val flags = AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL or AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS
        if (layoutParams.scrollFlags != flags) {
            layoutParams.scrollFlags = flags
            mainBinding.toolbar.layoutParams = layoutParams
        }
    }

    /**
     * Disable the whole scrolling system, which pins the toolbar and the FAB in place.
     * This is used when the content of the fragment is not big enough to require scrolling,
     * such is the case when a short list or an empty view is displayed.
     */
    fun disableScrolling(delay: Boolean) {
        // Delay the behavior change by 300 milliseconds to give time to the FAB to restore its default position
        mainBinding.fab.postDelayed({
            val layoutParams = mainBinding.toolbar.layoutParams as AppBarLayout.LayoutParams
            if (layoutParams.scrollFlags != 0) {
                layoutParams.scrollFlags = 0
                mainBinding.toolbar.layoutParams = layoutParams
            }
        }, if (delay) 300L else 0L)
    }

    fun fab(): FloatingActionButton {
        return mainBinding.fab
    }

    internal inner class ProfileIconTarget : Target {
        override fun onBitmapLoaded(bitmap: Bitmap, from: Picasso.LoadedFrom) {
            // Create another image the same size
            val padding = 48
            val imageWithBG = Bitmap.createBitmap(bitmap.width + padding, bitmap.height + padding, bitmap.config)
            // Create a canvas to draw on the new image
            val canvas = Canvas(imageWithBG)
            // Set its background to white
            canvas.drawColor(Color.WHITE)
            // Draw old image on the background
            canvas.drawBitmap(bitmap, padding / 2f, padding / 2f, null)
            profile.withIcon(imageWithBG)
            accountHeader.updateProfile(profile)
        }

        override fun onBitmapFailed(e: Exception?, errorDrawable: Drawable?) {
            Timber.e(e, "Failed to load profile icon")
        }

        override fun onPrepareLoad(placeHolderDrawable: Drawable) {}
    }

    // TODO: implement chooseAccount
    //    private void chooseAccount() {
    //        // disconnects the account and the account picker
    //        // stays there until another account is chosen
    //        if (mGoogleApiClient.isOnline()) {
    //            // Prior to disconnecting, run clearDefaultAccount().
    ////            Plus.AccountApi.clearDefaultAccount(mGoogleApiClient);
    ////            Plus.AccountApi.revokeAccessAndDisconnect(mGoogleApiClient)
    ////                    .setResultCallback(new ResultCallback<Status>() {
    ////
    ////                        public void onResult(Status status) {
    ////                            // mGoogleApiClient is now disconnected and access has been revoked.
    ////                            // Trigger app logic to comply with the developer policies
    ////                        }
    ////
    ////                    });
    //            Plus.AccountApi.clearDefaultAccount(mGoogleApiClient);
    //            mGoogleApiClient.disconnect();
    //            mGoogleApiClient.connect();
    //        }
    //    }

    // TODO: implement OnAccountAddComplete
    //    private class OnAccountAddComplete implements AccountManagerCallback<Bundle> {
    //        @Override
    //        public void run(AccountManagerFuture<Bundle> result) {
    //            Bundle bundle;
    //            try {
    //                bundle = result.getResult();
    //            } catch (OperationCanceledException e) {
    //                e.printStackTrace();
    //                return;
    //            } catch (AuthenticatorException e) {
    //                e.printStackTrace();
    //                return;
    //            } catch (IOException e) {
    //                e.printStackTrace();
    //                return;
    //            }
    //
    ////            MaterialAccount acc = new MaterialAccount(MainActivity.this.getResources(), "",
    ////                    bundle.getString(AccountManager.KEY_ACCOUNT_NAME), R.drawable.ic_photo, R.drawable.ic_cover);
    ////            MainActivity.this.addAccount(acc);
    //
    ////            mAccount = new Account(
    ////                    bundle.getString(AccountManager.KEY_ACCOUNT_NAME),
    ////                    bundle.getString(AccountManager.KEY_ACCOUNT_TYPE)
    ////            );
    //            // do more stuff
    //        }
    //    }
}
