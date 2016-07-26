package com.teo.ttasks.injection.component;

import com.teo.ttasks.injection.module.SignInModule;
import com.teo.ttasks.ui.activities.sign_in.SignInActivity;

import javax.inject.Singleton;

import dagger.Subcomponent;

@Singleton
@Subcomponent(modules = SignInModule.class)
public interface SignInComponent {

    void inject(SignInActivity signInActivity);
}
