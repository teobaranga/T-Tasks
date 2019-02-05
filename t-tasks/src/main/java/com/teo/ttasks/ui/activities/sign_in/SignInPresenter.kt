package com.teo.ttasks.ui.activities.sign_in

import com.androidhuman.rxfirebase2.auth.rxSignInWithCredential
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.teo.ttasks.data.local.PrefHelper
import com.teo.ttasks.data.model.TaskList
import com.teo.ttasks.data.remote.TasksHelper
import com.teo.ttasks.data.remote.TokenHelper
import com.teo.ttasks.ui.base.Presenter
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import timber.log.Timber

internal class SignInPresenter(
    private val tokenHelper: TokenHelper,
    private val tasksHelper: TasksHelper,
    private val prefHelper: PrefHelper
) : Presenter<SignInView>() {

    /**
     * Save some user info. Useful to detect when a user is signed in.
     *
     * @param account the current user's account
     */
    internal fun signIn(account: GoogleSignInAccount) {

        prefHelper.setUser(account.email!!, account.displayName!!)

        // First, refresh the access token
        val disposable = tokenHelper.refreshAccessToken(account.account!!)
            .observeOn(Schedulers.io())
            // Sign in using the acquired token
            .flatMap { FirebaseAuth.getInstance().rxSignInWithCredential(GoogleAuthProvider.getCredential(null, it)) }
            // Cache the user's photo URL
            .doOnSuccess { firebaseUser ->
                Timber.v("%s %s", firebaseUser.displayName, firebaseUser.email)
            }
            // Indicate that we're loading the task lists + tasks next
            .observeOn(AndroidSchedulers.mainThread())
            .doAfterSuccess { view()?.onLoadingTasks() }
            // Refresh the task lists
            .observeOn(Schedulers.io())
            .flatMapPublisher { tasksHelper.refreshTaskLists().map(TaskList::id) }
            // Refresh each task list
            .flatMapCompletable { tasksHelper.refreshTasks(it) }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeOn(Schedulers.io())
            .subscribe(
                { view()?.onSignInSuccess() },
                { throwable ->
                    Timber.e(throwable, "Error signing in")
                    view()?.onSignInError()
                })
        disposeOnUnbindView(disposable)
    }
}
