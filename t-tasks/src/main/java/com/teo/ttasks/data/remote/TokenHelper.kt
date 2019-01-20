package com.teo.ttasks.data.remote

import android.accounts.Account
import android.content.Context
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.common.Scopes
import com.teo.ttasks.data.local.PrefHelper
import com.teo.ttasks.injection.module.ApplicationModule.Companion.SCOPE_TASKS
import io.reactivex.Single

class TokenHelper(
    private val prefHelper: PrefHelper,
    private val context: Context
) {
    /**
     * Refresh the access token associated with the given account and cache it.
     *
     * @param account account of the user currently signed in
     * @return a Single containing the access token
     */
    fun refreshAccessToken(account: Account) = Single
        .fromCallable {
            // Clear the old token if it exists
            prefHelper.accessToken?.let { GoogleAuthUtil.clearToken(context, it) }

            // Return a new token
            // Note that GoogleAuthUtil.getToken is supposed to be an expensive network operation
            return@fromCallable GoogleAuthUtil.getToken(context, account, APP_SCOPES)
        }
        // Save the token to the preferences
        .doOnSuccess { prefHelper.accessToken = it }

    companion object {
        private const val APP_SCOPES = "oauth2:" + SCOPE_TASKS + " " + Scopes.PLUS_ME
    }
}
