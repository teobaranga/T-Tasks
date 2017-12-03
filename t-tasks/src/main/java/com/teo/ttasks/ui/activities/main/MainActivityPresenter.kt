package com.teo.ttasks.ui.activities.main

import android.support.v4.util.Pair
import com.teo.ttasks.api.PeopleApi
import com.teo.ttasks.data.local.PrefHelper
import com.teo.ttasks.data.remote.TasksHelper
import com.teo.ttasks.ui.base.Presenter
import com.teo.ttasks.util.NightHelper
import com.teo.ttasks.util.NightHelper.NIGHT_AUTO
import io.reactivex.android.schedulers.AndroidSchedulers
import io.realm.Realm
import timber.log.Timber

class MainActivityPresenter(private val tasksHelper: TasksHelper, private val prefHelper: PrefHelper, private val peopleApi: PeopleApi) : Presenter<MainView>() {

    private lateinit var realm: Realm

    internal val userName: String
        get() = prefHelper.userName

    internal val userEmail: String?
        get() = prefHelper.userEmail

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
        val subscription = peopleApi.getCurrentUserProfile()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ person ->
                    /**
                     * By default the profile url gives a 50x50 px image only,
                     * but we can replace the value with whatever dimension we want by replacing sz=X
                     */
                    val pictureUrl = person.image.url.split("\\?".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0]
                    if (pictureUrl != prefHelper.userPhoto) {
                        prefHelper.userPhoto = pictureUrl
                        view()?.onUserPicture(pictureUrl)
                    }

                    // Get cover picture
                    person.cover?.coverPhoto?.url?.let {
                        if (it != prefHelper.userCover) {
                            prefHelper.userCover = it
                            view()?.onUserCover(it)
                        }
                    }
                }, { Timber.e(it.toString()) })
        disposeOnUnbindView(subscription)
    }

    internal fun loadUserPictures() {
        prefHelper.userPhoto?.let { view()?.onUserPicture(it) }
        prefHelper.userCover?.let { view()?.onUserCover(it) }
    }

    internal fun getTaskLists() {
        val disposable = tasksHelper.getTaskLists(realm)
                .map { taskLists ->
                    val currentTaskListId = prefHelper.currentTaskListId
                    // Find the index of the current task list
                    taskLists.forEachIndexed { index, tTaskList ->
                        if (tTaskList.id == currentTaskListId)
                            Pair.create(taskLists, index)
                    }
                    Pair(taskLists, 0)
                }
                .subscribe({ taskListsIndexPair ->
                    view()?.onTaskListsLoaded(taskListsIndexPair.first!!, taskListsIndexPair.second!!)
                }, {
                    Timber.e(it.toString())
                    view()?.onTaskListsLoadError()
                })
        disposeOnUnbindView(disposable)
    }

    internal fun refreshTaskLists() {
        val subscription = tasksHelper.refreshTaskLists()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ /* do nothing */ }) { Timber.e(it.toString()) }
        disposeOnUnbindView(subscription)
    }

    internal fun clearUser() {
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
    }

    override fun unbindView(view: MainView) {
        super.unbindView(view)
        realm.close()
    }
}
