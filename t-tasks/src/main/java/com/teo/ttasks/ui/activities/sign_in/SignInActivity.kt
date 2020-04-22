package com.teo.ttasks.ui.activities.sign_in

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.google.android.gms.common.api.ApiException
import com.teo.ttasks.R
import com.teo.ttasks.UserManager
import com.teo.ttasks.databinding.ActivitySignInBinding
import com.teo.ttasks.receivers.NetworkInfoReceiver.Companion.isOnline
import com.teo.ttasks.ui.activities.main.MainActivity.Companion.startMainActivity
import com.teo.ttasks.util.toastShort
import org.koin.android.ext.android.inject
import org.koin.androidx.scope.lifecycleScope
import timber.log.Timber

open class SignInActivity : AppCompatActivity(), SignInView {

    companion object {

        private const val RC_SIGN_IN = 0

        private const val ARG_SIGN_OUT = "signOut"

        fun Context.startSignInActivity(signOut: Boolean) =
            startActivity(Intent(this, SignInActivity::class.java).apply { putExtra(ARG_SIGN_OUT, signOut) })
    }

    private val signInPresenter: SignInPresenter by lifecycleScope.inject()

    private val userManager: UserManager by inject()

    private lateinit var signInBinding: ActivitySignInBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        signInBinding = DataBindingUtil.setContentView(this, R.layout.activity_sign_in)
        signInPresenter.bindView(this)

        signInBinding.signInButton.setOnClickListener {
            if (!isOnline()) {
                toastShort(R.string.error_sign_in_offline)
            } else {
                // Trigger the sign in flow
                val signInIntent = userManager.signInIntent
                startActivityForResult(signInIntent, RC_SIGN_IN)

                signInBinding.loadingText.setText(R.string.signing_in)
                signInBinding.viewSwitcher.showNext()
            }
        }
    }

    override fun onDestroy() {
        signInPresenter.unbindView(this)
        super.onDestroy()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
            RC_SIGN_IN -> {
                if (resultCode == Activity.RESULT_OK) {
                    try {
                        val account = userManager.getSignedInAccountFromIntent(data)
                        signInPresenter.signIn(account)
                    } catch (e: ApiException) {
                        Timber.e(e, "Error signing in")
                        toastShort(R.string.error_sign_in)
                        signInBinding.viewSwitcher.showPrevious()
                    }
                } else {
                    // Google Sign In cancelled
                    signInBinding.viewSwitcher.showPrevious()
                }
            }
        }
    }

    override fun onLoadingTasks() {
        signInBinding.loadingText.setText(R.string.loading_tasks)
    }

    override fun onSignInSuccess() {
        startMainActivity()
        finish()
    }

    override fun onSignInError() {
        signInBinding.viewSwitcher.showPrevious()
        toastShort(R.string.error_sign_in)
    }
}
