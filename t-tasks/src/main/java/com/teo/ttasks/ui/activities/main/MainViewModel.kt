package com.teo.ttasks.ui.activities.main

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.map
import com.androidhuman.rxfirebase2.auth.rxSignOut
import com.google.firebase.auth.FirebaseAuth
import com.squareup.picasso.Picasso
import com.squareup.picasso.Target
import com.teo.ttasks.BuildConfig
import com.teo.ttasks.LiveRealmResults
import com.teo.ttasks.UserManager
import com.teo.ttasks.data.local.PrefHelper
import com.teo.ttasks.data.remote.TasksHelper
import com.teo.ttasks.livedata.Event
import com.teo.ttasks.ui.base.RealmViewModel
import com.teo.ttasks.util.NightHelper
import io.reactivex.disposables.CompositeDisposable
import org.koin.core.KoinComponent
import org.koin.core.inject
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream

class MainViewModel : RealmViewModel(), KoinComponent {

    enum class ActionEvent {
        ADD_TASK,
        ADD_TASK_LIST,
        ABOUT,
        SETTINGS,
    }

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

    private var profileIconTarget: ProfileIconTarget? = null

    private val firebaseAuth: FirebaseAuth by inject()

    private val prefHelper: PrefHelper by inject()

    private val tasksHelper: TasksHelper by inject()

    private val userManager: UserManager by inject()

    val taskLists = LiveRealmResults(tasksHelper.getTaskListFromRealm(realm))

    val activeTaskList = taskLists.map { list ->
        val activeTaskListId = prefHelper.currentTaskListId
        if (activeTaskListId != null) {
            return@map list.firstOrNull { it.id == activeTaskListId }
        } else {
            return@map list.firstOrNull()
        }
    }.distinctUntilChanged()

    /**
     * Save the ID of the last accessed task list so that it can be displayed the next time the
     * user opens the app.
     */
    var activeTaskListId: String?
        get() = prefHelper.currentTaskListId
        set(id) {
            prefHelper.currentTaskListId = id
        }

    private val _signedIn = MutableLiveData<Boolean>()
    val signedIn: LiveData<Boolean> = _signedIn

    /** Listener handling the sign out event */
    private val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        _signedIn.value = firebaseAuth.currentUser != null

        if (firebaseAuth.currentUser == null) {
            // The user has signed out
            // TODO Unregister all task listeners

            // Clear the user's preferences
            prefHelper.clearUser()
            // Clear the database
            realm.executeTransaction { it.deleteAll() }
            // Reset the night mode
            NightHelper.applyNightMode(NightHelper.NIGHT_AUTO)
        }
    }

    private val _profilePicture = MutableLiveData<Drawable?>()
    val profilePicture: LiveData<Drawable?> = _profilePicture

    private val _events = MutableLiveData<Event<ActionEvent>>()
    val events: LiveData<Event<ActionEvent>> = _events

    private val disposables = CompositeDisposable()

    init {
        firebaseAuth.addAuthStateListener(authStateListener)

        tasksHelper.refreshTaskLists()
            .ignoreElements()
            .subscribe(
                {
                    /* nothing to do, Realm takes care of update notifications */
                },
                {
                    Timber.e(it, "Error refreshing task lists")
                })

        firebaseAuth.currentUser?.let { firebaseUser ->
            val photoFile = File("/data/data/${BuildConfig.APPLICATION_ID}/cache", firebaseUser.uid)
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
                            _profilePicture.value = BitmapDrawable(null, bitmap)

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
                    _profilePicture.value = BitmapDrawable(null, photoFile.absolutePath)
                }
                else -> {
                    Timber.w("Profile photo not found on disk, resetting photo URL in preferences")
                    prefHelper.userPhoto = null
                }
            }
        }

    }

    fun onAddTaskClicked() {
        _events.value = Event(ActionEvent.ADD_TASK)
    }

    fun onAddTaskListClicked() {
        _events.value = Event(ActionEvent.ADD_TASK_LIST)
    }

    fun onAboutClicked() {
        _events.value = Event(ActionEvent.ABOUT)
    }

    fun onSettingsClicked() {
        _events.value = Event(ActionEvent.SETTINGS)
    }

    fun onSignOutClicked() {

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
        disposables.add(disposable)
    }

    internal fun isSignedIn(): Boolean {
        return firebaseAuth.currentUser != null
    }

    override fun onCleared() {
        disposables.clear()
        firebaseAuth.removeAuthStateListener(authStateListener)
        super.onCleared()
    }
}
