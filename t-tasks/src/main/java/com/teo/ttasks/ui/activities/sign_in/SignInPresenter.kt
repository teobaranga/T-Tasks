package com.teo.ttasks.ui.activities.sign_in

import com.androidhuman.rxfirebase2.auth.rxSignInWithCredential
import com.google.android.gms.auth.UserRecoverableAuthException
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.teo.ttasks.data.local.PrefHelper
import com.teo.ttasks.data.remote.TasksHelper
import com.teo.ttasks.data.remote.TokenHelper
import com.teo.ttasks.ui.base.Presenter
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import timber.log.Timber

internal class SignInPresenter(private val mTokenHelper: TokenHelper,
                               private val mTasksHelper: TasksHelper,
                               private val mPrefHelper: PrefHelper) : Presenter<SignInView>() {

    /**
     * Save some user info. Useful to detect when a user is signed in.

     * @param account the current user's account
     */
    internal fun saveUser(account: GoogleSignInAccount) {
        mPrefHelper.setUser(account.email!!, account.displayName!!)
    }

    internal fun saveToken(token: String) {
        mPrefHelper.accessToken = token
    }

    internal fun signIn(firebaseAuth: FirebaseAuth) {
        val disposable = mTokenHelper.refreshAccessToken()
                .flatMap<FirebaseUser> { accessToken ->
                    // Sign in using the acquired token
                    val credential = GoogleAuthProvider.getCredential(null, accessToken)
                    return@flatMap firebaseAuth.rxSignInWithCredential(credential)
                }
                .doOnSuccess { firebaseUser ->
                    Timber.d("%s %s", firebaseUser.displayName, firebaseUser.email)
                    // Indicate that we're loading the task lists next
                    view()?.onLoadingTaskLists()
                }
                .flatMapPublisher { mTasksHelper.refreshTaskLists() }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    view()?.onSignInSuccess()
                }, { throwable ->
                    Timber.e("Error signing in: %s", throwable.toString())
                    if (throwable.cause is UserRecoverableAuthException) {
                        view()?.onSignInError((throwable.cause as UserRecoverableAuthException).intent)
                    } else {
                        view()?.onSignInError(null)
                    }
                })
        disposeOnUnbindView(disposable)
    }
}
