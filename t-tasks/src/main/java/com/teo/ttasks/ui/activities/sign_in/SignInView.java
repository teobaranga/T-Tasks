package com.teo.ttasks.ui.activities.sign_in;

import android.content.Intent;
import android.support.annotation.Nullable;

import com.teo.ttasks.ui.base.MvpView;

interface SignInView extends MvpView {

    void onSignInSuccess();

    void onSignInError(@Nullable Intent resolveIntent);
}
