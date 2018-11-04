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
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import io.realm.Realm
import timber.log.Timber

internal class SignInPresenter(private val tokenHelper: TokenHelper,
                               private val tasksHelper: TasksHelper,
                               private val prefHelper: PrefHelper) : Presenter<SignInView>() {

    /**
     * Save some user info. Useful to detect when a user is signed in.
     *
     * @param account the current user's account
     */
    internal fun signIn(account: GoogleSignInAccount, firebaseAuth: FirebaseAuth) {

        prefHelper.setUser(account.email!!, account.displayName!!)

        // First, refresh the access token
        val disposable = tokenHelper.refreshAccessToken()
                .observeOn(Schedulers.io())
                .flatMap { accessToken ->
                    // Sign in using the acquired token
                    val credential = GoogleAuthProvider.getCredential(null, accessToken)
                    return@flatMap firebaseAuth.rxSignInWithCredential(credential)
                            .doOnSuccess { firebaseUser ->
                                prefHelper.userPhoto = firebaseUser.photoUrl.toString()
                                Timber.v("%s %s", firebaseUser.displayName, firebaseUser.email)
                                Timber.v("Photo URL: %s", prefHelper.userPhoto)
                            }
                }
                .observeOn(AndroidSchedulers.mainThread())
                .doAfterSuccess {
                    // Indicate that we're loading the task lists + tasks next
                    view()?.onLoadingTasks()
                }
                .observeOn(Schedulers.io())
                .flatMapPublisher { _ ->
                    // Refresh the task lists
                    return@flatMapPublisher tasksHelper.refreshTaskLists()
                            .andThen(Flowable.defer {
                                // For each task list, get its ID
                                val realm = Realm.getDefaultInstance()
                                return@defer Single.just(tasksHelper.queryTaskLists(realm).findAll())
                                        .flattenAsFlowable { it }
                                        .map { it.id }
                                        .doFinally { realm.close() }
                            })
                }
                .flatMapCompletable { taskListId ->
                    // Refresh each task list
                    return@flatMapCompletable tasksHelper.refreshTasks(taskListId)
                }
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
