package com.teo.ttasks.data.remote

import android.accounts.Account
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE
import com.google.android.gms.common.Scopes
import com.teo.ttasks.TTasksApp
import com.teo.ttasks.data.local.PrefHelper
import com.teo.ttasks.injection.module.ApplicationModule.Companion.SCOPE_TASKS
import io.reactivex.Single
import timber.log.Timber

class TokenHelper(private val prefHelper: PrefHelper, private val tTasksApp: TTasksApp) {

    companion object {
        private const val APP_SCOPES = "oauth2:" + SCOPE_TASKS + " " + Scopes.PLUS_ME
    }

    init {
        prefHelper.userEmail?.let { email -> account = Account(email, GOOGLE_ACCOUNT_TYPE) }
    }

    /** The current user's Google account */
    private var account: Account? = null

    val token: String
        get() {
            account = account ?: Account(prefHelper.userEmail!!, GOOGLE_ACCOUNT_TYPE)
            return GoogleAuthUtil.getToken(tTasksApp, account!!, APP_SCOPES)
        }

    /**
     * Get the access token and save it
     *
     * @return a Single containing the access token
     */
    fun refreshAccessToken(): Single<String> {
        account = account ?: Account(prefHelper.userEmail!!, GOOGLE_ACCOUNT_TYPE)
        return Single
                .fromCallable {
                    // Clear the old token if it exists
                    prefHelper.accessToken?.let { token -> GoogleAuthUtil.clearToken(tTasksApp, token) }
                    // Return a Single with the new token
                    return@fromCallable GoogleAuthUtil.getToken(tTasksApp, account!!, APP_SCOPES)

                }
                .doOnSuccess {
                    // Save the token to the preferences
                    prefHelper.accessToken = it
                    Timber.d(it)
                }
    }
}
