package com.teo.ttasks.ui.activities.sign_in;

import com.teo.ttasks.ui.base.MvpView;

interface SignInView extends MvpView {

    void onSignInSuccess();

    void onSignInError();
}
