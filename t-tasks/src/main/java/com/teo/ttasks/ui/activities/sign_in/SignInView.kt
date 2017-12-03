package com.teo.ttasks.ui.activities.sign_in

import android.content.Intent

import com.teo.ttasks.ui.base.MvpView

internal interface SignInView : MvpView {

    fun onLoadingTaskLists()

    fun onSignInSuccess()

    fun onSignInError(resolveIntent: Intent?)
}
