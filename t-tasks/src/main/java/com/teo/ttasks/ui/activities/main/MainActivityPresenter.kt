package com.teo.ttasks.ui.activities.main

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.view.MenuItem
import com.androidhuman.rxfirebase2.auth.rxSignOut
import com.google.firebase.auth.FirebaseAuth
import com.squareup.picasso.Picasso
import com.squareup.picasso.Target
import com.teo.ttasks.UserManager
import com.teo.ttasks.data.local.PrefHelper
import com.teo.ttasks.data.model.TaskList
import com.teo.ttasks.data.remote.TasksHelper
import com.teo.ttasks.ui.base.Presenter
import com.teo.ttasks.util.NightHelper
import com.teo.ttasks.util.NightHelper.NIGHT_AUTO
import io.reactivex.android.schedulers.AndroidSchedulers
import io.realm.Realm
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream

class MainActivityPresenter(
    private val context: Context,
    private val tasksHelper: TasksHelper,
    private val prefHelper: PrefHelper,
    private val userManager: UserManager,
    private val firebaseAuth: FirebaseAuth
) : Presenter<MainView>() {

    private open class ProfileIconTarget(private val targetFile: File) : Target {

        override fun onBitmapLoaded(bitmap: Bitmap, from: Picasso.LoadedFrom) {
            FileOutputStream(targetFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
        }

        override fun onBitmapFailed(e: Exception?, errorDrawable: Drawable?) {
            Timber.e(e, "Failed to load profile icon")
        }

        override fun onPrepareLoad(placeHolderDrawable: Drawable?) {}
    }

    private lateinit var realm: Realm

    /**
     * Save the ID of the last accessed task list so that it can be displayed the next time the user opens the app
     *
     * @param taskListId task list identifier
     */
    internal var lastAccessedTaskListId: String?
        get() = prefHelper.currentTaskListId
        set(value) {
            prefHelper.currentTaskListId = value
        }

    /** Listener handling the sign out event */
    private val firebaseAuthStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        if (firebaseAuth.currentUser == null) {
            // The user has signed out
            // TODO Unregister all task listeners
            clearUser()
            view()?.onSignedOut()
        }
    }

    private var profileIconTarget: ProfileIconTarget? = null

    internal fun isSignedIn() = firebaseAuth.currentUser != null

    internal fun loadProfilePicture(menuItem: MenuItem) {
        firebaseAuth.currentUser?.let { firebaseUser ->
            val photoFile = File(context.cacheDir, firebaseUser.uid)
            val photoUrl = firebaseUser.photoUrl.toString()
            when {
                photoUrl != prefHelper.userPhoto -> {
                    Timber.v("New profile photo: %s", photoUrl)
                    profileIconTarget = object : ProfileIconTarget(photoFile) {
                        override fun onBitmapLoaded(bitmap: Bitmap, from: Picasso.LoadedFrom) {
                            super.onBitmapLoaded(bitmap, from)

                            // Load successful, save the URL to preferences
                            prefHelper.userPhoto = photoUrl

                            // Apply the bitmap
                            menuItem.icon = BitmapDrawable(context.resources, bitmap)

                            profileIconTarget = null
                        }
                    }.also { target ->
                        Picasso.get()
                            .load(photoUrl)
                            .into(target)
                    }
                }
                photoFile.exists() -> {
                    Timber.v("Loading profile photo from disk")
                    menuItem.icon = BitmapDrawable(context.resources, photoFile.absolutePath)
                }
                else -> {
                    Timber.w("Profile photo not found on disk, resetting photo URL in preferences")
                    prefHelper.userPhoto = null
                }
            }
        }
    }

    internal fun subscribeToTaskLists() {
        Timber.v("Subscribing to task lists notification")

        val disposable = tasksHelper.getTaskLists(realm, false)
            .filter { it.isNotEmpty() }
            .subscribe(
                { taskLists ->
                    // Find the index of the current task list
                    val currentTaskListId = prefHelper.currentTaskListId
                    val index = taskLists.indexOfFirst { it.id == currentTaskListId }.coerceAtLeast(0)
                    view()?.onTaskListsLoaded(taskLists, index)
                },
                {
                    Timber.e(it, "Error while loading task lists")
                    view()?.onTaskListsLoadError()
                })
        disposeOnUnbindView(disposable)
    }

    internal fun getTaskLists(): Pair<List<TaskList>, Int> {
        val taskLists = tasksHelper.getTaskLists(realm, false).blockingFirst()

        // Find the index of the current task list
        val currentTaskListId = prefHelper.currentTaskListId
        val index = taskLists.indexOfFirst { it.id == currentTaskListId }.coerceAtLeast(0)

        return Pair(taskLists, index)
    }

    internal fun refreshTaskLists() {
        val subscription = tasksHelper.refreshTaskLists()
            .ignoreElements()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ /* do nothing */ }) { Timber.e(it, "Error refreshing task lists") }
        disposeOnUnbindView(subscription)
    }

    internal fun signOut() {
        val disposable = firebaseAuth.rxSignOut()
            .onErrorComplete {
                Timber.e(it, "There was an error signing out from Firebase, ignoring")
                return@onErrorComplete true
            }
            .andThen(userManager.signOut())
            .onErrorComplete {
                Timber.e(it, "There was an error signing out from Google, ignoring")
                return@onErrorComplete true
            }
            .subscribe({
                Timber.d("Signed out")
            }, {
                Timber.e(it, "Could not sign out")
            })
        disposeOnUnbindView(disposable)
    }

    private fun clearUser() {
        // Clear the user's preferences
        prefHelper.clearUser()
        // Clear the database
        realm.executeTransaction { it.deleteAll() }
        // Reset the night mode
        NightHelper.applyNightMode(NIGHT_AUTO)
    }

    override fun bindView(view: MainView) {
        super.bindView(view)
        realm = Realm.getDefaultInstance()
        firebaseAuth.addAuthStateListener(firebaseAuthStateListener)
    }

    override fun unbindView(view: MainView) {
        firebaseAuth.removeAuthStateListener(firebaseAuthStateListener)
        realm.close()
        super.unbindView(view)
    }
}
