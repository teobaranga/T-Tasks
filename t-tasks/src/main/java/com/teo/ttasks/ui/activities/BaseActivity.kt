package com.teo.ttasks.ui.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat.startActivityForResult
import com.google.android.gms.auth.UserRecoverableAuthException
import com.teo.ttasks.R
import com.teo.ttasks.data.remote.TokenHelper
import com.teo.ttasks.ui.activities.sign_in.SignInActivity
import dagger.android.support.DaggerAppCompatActivity
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import javax.inject.Inject

abstract class BaseActivity : DaggerAppCompatActivity() {

    @Inject internal lateinit var mTokenHelper: TokenHelper

    private var toolbar: Toolbar? = null

    override fun setContentView(layoutResID: Int) {
        super.setContentView(layoutResID)
        setupToolbar()
    }

    override fun setContentView(view: View) {
        super.setContentView(view)
        setupToolbar()
    }

    override fun setContentView(view: View, params: ViewGroup.LayoutParams) {
        super.setContentView(view, params)
        setupToolbar()
    }

    private fun setupToolbar() {
        toolbar = findViewById(R.id.toolbar)
        toolbar?.let { toolbar -> setSupportActionBar(toolbar) }
    }

    fun toolbar(): Toolbar? = toolbar

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)

        // TODO: 2016-07-25 is this necessary?
        Single.defer { Single.just(mTokenHelper.token) }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        { token ->
                            Timber.v("Token: %s", token)
                            onApiReady()
                        },
                        { throwable ->
                            when (throwable) {
                                is UserRecoverableAuthException -> {
                                    startActivityForResult(throwable.intent, RC_USER_RECOVERABLE)
                                }
                                else -> {
                                    Timber.e(throwable, "Error while retrieving token")
                                    // TODO: 2016-07-25 probably an error cause by a missing internet connection
                                    // TODO: 2016-07-25 not sure what to do in this case
                                }
                            }
                        }
                )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_USER_RECOVERABLE) {
            if (resultCode == Activity.RESULT_OK)
                onApiReady()
            else {
                // User denied permission to his tasks, redirect to the sign in screen :(
                SignInActivity.start(this, false)
                finish()
            }
        }
    }

    protected abstract fun onApiReady()

    companion object {
        private const val RC_USER_RECOVERABLE = 1002
    }
}
