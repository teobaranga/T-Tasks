package com.teo.ttasks.ui.activities.sign_in

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.widget.Toast
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.Scopes
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.common.api.Scope
import com.google.firebase.auth.FirebaseAuth
import com.teo.ttasks.R
import com.teo.ttasks.databinding.ActivitySignInBinding
import com.teo.ttasks.injection.module.ApplicationModule.Companion.SCOPE_TASKS
import com.teo.ttasks.receivers.NetworkInfoReceiver
import com.teo.ttasks.ui.activities.main.MainActivity
import dagger.android.support.DaggerAppCompatActivity
import timber.log.Timber
import javax.inject.Inject

open class SignInActivity : DaggerAppCompatActivity(), SignInView, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    /** Bool to track whether the app is already resolving an error  */
    private var resolvingError = false

    @Inject internal lateinit var signInPresenter: SignInPresenter
    @Inject internal lateinit var networkInfoReceiver: NetworkInfoReceiver

    private val firebaseAuth by lazy {
        FirebaseAuth.getInstance()
    }

    private val googleApiClient by lazy {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestScopes(Scope(SCOPE_TASKS), Scope(Scopes.PLUS_ME))
                .build()

        return@lazy GoogleApiClient.Builder(this)
                .enableAutoManage(this, this)
                .addConnectionCallbacks(this)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build()
    }

    private lateinit var signInBinding: ActivitySignInBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        signInBinding = DataBindingUtil.setContentView(this, R.layout.activity_sign_in)
        signInPresenter.bindView(this)

        signInBinding.signInButton.setOnClickListener {
            if (!networkInfoReceiver.isOnline(this)) {
                Toast.makeText(this, R.string.error_sign_in_offline, Toast.LENGTH_SHORT).show()
            } else {
                // Trigger the sign in flow
                val signInIntent = Auth.GoogleSignInApi.getSignInIntent(googleApiClient)
                startActivityForResult(signInIntent, RC_SIGN_IN)
            }
        }
    }

    override fun onDestroy() {
        signInPresenter.unbindView(this)
        super.onDestroy()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        super.onActivityResult(requestCode, resultCode, data)
        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        when (requestCode) {
            RC_RESOLVE_ERROR -> {
                resolvingError = false
                if (resultCode == Activity.RESULT_OK) {
                    // Make sure the app is not already connected or attempting to connect
                    if (!googleApiClient.isConnecting && !googleApiClient.isConnected)
                        googleApiClient.connect()
                }
                return
            }
            RC_SIGN_IN -> {
                val result = Auth.GoogleSignInApi.getSignInResultFromIntent(data)
                if (result.isSuccess) {
                    signInBinding.loadingText.setText(R.string.signing_in)
                    signInBinding.viewSwitcher.showNext()
                    val account = result.signInAccount!!
                    signInPresenter.saveUser(account)
                    signInPresenter.signIn(firebaseAuth)
                } else {
                    if (result.status.statusCode != GoogleSignInStatusCodes.SIGN_IN_CANCELLED) {
                        Timber.e(result.status.toString())
                        Toast.makeText(this, R.string.error_sign_in, Toast.LENGTH_SHORT).show()
                    }
                    signInBinding.viewSwitcher.showPrevious()
                }
                return
            }
            RC_USER_RECOVERABLE -> {
                if (resultCode == Activity.RESULT_OK) {
                    signInPresenter.signIn(firebaseAuth)
                    return
                }
                Toast.makeText(this, R.string.error_google_permissions_denied, Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onLoadingTasks() {
        signInBinding.loadingText.setText(R.string.loading_tasks)
    }

    override fun onSignInSuccess() {
        MainActivity.start(this)
        finish()
    }

    override fun onSignInError(resolveIntent: Intent?) {
        signInBinding.viewSwitcher.showPrevious()
        if (resolveIntent != null) {
            startActivityForResult(resolveIntent, RC_USER_RECOVERABLE)
        } else {
            Toast.makeText(this, R.string.error_sign_in, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onConnected(bundle: Bundle?) {
        intent?.let { intent ->
            if (intent.getBooleanExtra(ARG_SIGN_OUT, false)) {
                Auth.GoogleSignInApi.signOut(googleApiClient).setResultCallback { status -> Timber.d(status.toString()) }
            }
        }
    }

    override fun onConnectionSuspended(i: Int) {
        // Do nothing
    }

    override fun onConnectionFailed(result: ConnectionResult) {
        if (!resolvingError) {
            if (result.hasResolution()) {
                try {
                    resolvingError = true
                    result.startResolutionForResult(this, RC_RESOLVE_ERROR)
                } catch (e: IntentSender.SendIntentException) {
                    // There was an error with the resolution intent. Try again.
                    googleApiClient.connect()
                }

            } else {
                // Show dialog using GoogleApiAvailability.getErrorDialog()
                showGooglePlayServicesAvailabilityErrorDialog(result.errorCode)
                resolvingError = true
            }
        }
    }

    /**
     * Display an error dialog showing that Google Play Services is missing
     * or out of date.
     *
     * @param connectionStatusCode code describing the presence (or lack of)
     *                             Google Play Services on this device.
     */
    private fun showGooglePlayServicesAvailabilityErrorDialog(connectionStatusCode: Int) {
        val googleAPI = GoogleApiAvailability.getInstance()
        if (googleAPI.isUserResolvableError(connectionStatusCode)) {
            val dialog = googleAPI.getErrorDialog(this, connectionStatusCode, RC_RESOLVE_ERROR)
            dialog.setOnDismissListener { resolvingError = false }
            dialog.show()
        }
    }

    companion object {

        /** Request code to use when launching the resolution activity  */
        private const val RC_RESOLVE_ERROR = 1001
        private const val RC_SIGN_IN = 0
        private const val RC_USER_RECOVERABLE = 1

        private const val ARG_SIGN_OUT = "signOut"

        fun start(context: Context, signOut: Boolean) {
            val starter = Intent(context, SignInActivity::class.java)
            starter.putExtra(ARG_SIGN_OUT, signOut)
            context.startActivity(starter)
        }
    }
}
