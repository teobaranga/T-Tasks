package com.teo.ttasks.ui.activities.main

import androidx.core.util.Pair
import com.androidhuman.rxfirebase2.auth.rxSignOut
import com.google.firebase.auth.FirebaseAuth
import com.teo.ttasks.UserManager
import com.teo.ttasks.api.PeopleApi
import com.teo.ttasks.data.local.PrefHelper
import com.teo.ttasks.data.remote.TasksHelper
import com.teo.ttasks.ui.base.Presenter
import com.teo.ttasks.util.NightHelper
import com.teo.ttasks.util.NightHelper.NIGHT_AUTO
import io.reactivex.android.schedulers.AndroidSchedulers
import io.realm.Realm
import timber.log.Timber

class MainActivityPresenter(
    private val tasksHelper: TasksHelper,
    private val prefHelper: PrefHelper,
    private val peopleApi: PeopleApi,
    private val userManager: UserManager
) : Presenter<MainView>() {

    private lateinit var realm: Realm

    internal val userName: String
        get() = prefHelper.userName

    internal val userEmail: String?
        get() = prefHelper.userEmail

    private val firebaseAuth = FirebaseAuth.getInstance()

    /** Listener handling the sign out event */
    private val firebaseAuthStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        if (firebaseAuth.currentUser == null) {
            // The user has signed out
            // TODO Unregister all task listeners
            clearUser()
            view()?.onSignedOut()
        }
    }

    internal fun isSignedIn() = firebaseAuth.currentUser != null

    /**
     * Save the ID of the last accessed task list so that it can be displayed the next time the user opens the app

     * @param taskListId task list identifier
     */
    internal fun setLastAccessedTaskList(taskListId: String) {
        prefHelper.currentTaskListId = taskListId
    }

    /**
     * Load the account information for the currently signed in Google user.
     * Must be called after onConnected
     */
    internal fun loadCurrentUser() {
        // Load the current user's profile picture
        firebaseAuth.currentUser?.photoUrl.apply { view()?.onUserPicture(toString()) }

        // Load the current user's cover photo
        val coverPhotoDisposable = peopleApi.getCurrentPersonCoverPhotos()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { coverPhotosResponse ->
                    val coverPhotos = coverPhotosResponse.coverPhotos
                    Timber.v("Found %d cover photo(s)", coverPhotos.size)
                    coverPhotos.getOrNull(0)?.apply {
                        Timber.v("Cover photo: %s", url)
                        view()?.onUserCover(url)
                    }
                },
                { Timber.e(it, "Error while loading cover photos") })
        disposeOnUnbindView(coverPhotoDisposable)
    }

    internal fun loadUserPictures() {
//        prefHelper.userPhoto?.let { view()?.onUserPicture(it) }
        prefHelper.userCover?.let { view()?.onUserCover(it) }
    }

    internal fun getTaskLists() {
        val disposable = tasksHelper.getTaskLists(realm)
            .filter { it.isNotEmpty() }
            .map { taskLists ->
                val currentTaskListId = prefHelper.currentTaskListId
                // Find the index of the current task list
                taskLists.forEachIndexed { index, taskList ->
                    if (taskList.id == currentTaskListId) {
                        return@map Pair.create(taskLists, index)
                    }
                }
                return@map Pair(taskLists, 0)
            }
            .subscribe(
                { taskListsIndexPair ->
                    view()?.onTaskListsLoaded(taskListsIndexPair.first!!, taskListsIndexPair.second!!)
                },
                {
                    Timber.e(it, "Error while loading task lists")
                    view()?.onTaskListsLoadError()
                })
        disposeOnUnbindView(disposable)
    }

    internal fun refreshTaskLists() {
        val subscription = tasksHelper.refreshTaskLists()
            .ignoreElements()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ /* do nothing */ }) { Timber.e(it, "Error refreshing task lists") }
        disposeOnUnbindView(subscription)
    }

    internal fun signOut() {
        firebaseAuth.rxSignOut()
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
