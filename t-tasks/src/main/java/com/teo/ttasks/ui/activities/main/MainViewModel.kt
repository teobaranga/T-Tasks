package com.teo.ttasks.ui.activities.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.map
import com.google.firebase.auth.FirebaseAuth
import com.teo.ttasks.LiveRealmResults
import com.teo.ttasks.data.local.PrefHelper
import com.teo.ttasks.data.remote.TasksHelper
import com.teo.ttasks.livedata.Event
import com.teo.ttasks.ui.base.RealmViewModel
import com.teo.ttasks.util.NightHelper
import org.koin.core.KoinComponent
import org.koin.core.inject
import timber.log.Timber

class MainViewModel : RealmViewModel(), KoinComponent {

    enum class ActionEvent {
        ABOUT,
        SETTINGS,
        SIGN_OUT
    }

    private val firebaseAuth: FirebaseAuth by inject()

    private val prefHelper: PrefHelper by inject()

    private val tasksHelper: TasksHelper by inject()

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

    val navigateTo = MutableLiveData<Event<ActionEvent>>()

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
    }

    internal fun isSignedIn(): Boolean {
        return firebaseAuth.currentUser != null
    }

    override fun onCleared() {
        firebaseAuth.removeAuthStateListener(authStateListener)
        super.onCleared()
    }
}
