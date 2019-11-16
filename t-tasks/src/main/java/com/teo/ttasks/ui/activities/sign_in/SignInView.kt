package com.teo.ttasks.ui.activities.sign_in

import com.teo.ttasks.ui.base.MvpView

internal interface SignInView : MvpView {

    fun onLoadingTasks()

    fun onSignInSuccess()

    fun onSignInError()
}
