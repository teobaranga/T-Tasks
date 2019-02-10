package com.teo.ttasks

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.Scopes
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Task
import com.teo.ttasks.injection.module.ApplicationModule
import io.reactivex.Completable

/**
 * Manages users (sign in / out).
 *
 * @param context the application context
 */
@OpenClassOnDebug
class UserManager(private val context: Context) {

    val signInIntent: Intent
        get() = googleSignInClient.signInIntent

    private val googleSignInClient by lazy {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestScopes(Scope(ApplicationModule.SCOPE_TASKS), Scope(Scopes.PLUS_ME))
            .build()

        return@lazy GoogleSignIn.getClient(context, gso)
    }

    private var signedInUser: GoogleSignInAccount? = null

    /**
     * @throws ApiException if the user could not be extracted from the intent
     */
    @Throws(ApiException::class)
    fun getSignedInAccountFromIntent(intent: Intent?): GoogleSignInAccount {
        val account = GoogleSignIn.getSignedInAccountFromIntent(intent).getResult(ApiException::class.java)!!
        signedInUser = account
        return account
    }

    fun signOut() = Completable.create { emitter ->
        googleSignInClient.signOut()
            .addOnCompleteListener { task: Task<Void> ->
                if (task.isSuccessful) {
                    emitter.onComplete()
                } else {
                    emitter.onError(task.exception!!)
                }
            }
    }
}
