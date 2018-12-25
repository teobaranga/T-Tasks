package com.teo.ttasks.ui.activities.sign_in

import com.androidhuman.rxfirebase2.auth.rxSignInWithCredential
import com.google.android.gms.auth.UserRecoverableAuthException
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.teo.ttasks.data.local.PrefHelper
import com.teo.ttasks.data.remote.TasksHelper
import com.teo.ttasks.data.remote.TokenHelper
import com.teo.ttasks.ui.base.Presenter
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import timber.log.Timber

internal class SignInPresenter(
        private val tokenHelper: TokenHelper,
        private val tasksHelper: TasksHelper,
        private val prefHelper: PrefHelper) : Presenter<SignInView>() {

    /**
     * Save some user info. Useful to detect when a user is signed in.
     *
     * @param account the current user's account
     */
    internal fun signIn(account: GoogleSignInAccount) {

        prefHelper.setUser(account.email!!, account.displayName!!)

        // First, refresh the access token
        val disposable = tokenHelper.refreshAccessToken()
                .observeOn(Schedulers.io())
                .flatMap { accessToken ->
                    // Sign in using the acquired token
                    val credential = GoogleAuthProvider.getCredential(null, accessToken)
                    return@flatMap FirebaseAuth.getInstance().rxSignInWithCredential(credential)
                            .doOnSuccess { firebaseUser ->
                                prefHelper.userPhoto = firebaseUser.photoUrl.toString()
                                Timber.v("%s %s", firebaseUser.displayName, firebaseUser.email)
                                Timber.v("Photo URL: %s", prefHelper.userPhoto)
                            }
                }
                .observeOn(AndroidSchedulers.mainThread())
                // Indicate that we're loading the task lists + tasks next
                .doAfterSuccess { view()?.onLoadingTasks() }
                // Refresh the task lists
                .observeOn(Schedulers.io())
                .flatMapPublisher { tasksHelper.refreshTaskLists().map { taskList -> taskList.id } }
                // Refresh each task list
                .flatMapCompletable { taskListId -> tasksHelper.refreshTasks(taskListId) }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(
                        { view()?.onSignInSuccess() },
                        { throwable ->
                            Timber.e(throwable, "Error signing in")
                            val exception = throwable.cause
                            when (exception) {
                                is UserRecoverableAuthException -> view()?.onSignInError(exception.intent)
                                else -> view()?.onSignInError(null)
                            }
                        })
        disposeOnUnbindView(disposable)
    }
}
