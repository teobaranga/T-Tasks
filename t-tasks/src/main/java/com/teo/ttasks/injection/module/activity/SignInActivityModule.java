package com.teo.ttasks.injection.module.activity;

import com.teo.ttasks.data.local.PrefHelper;
import com.teo.ttasks.data.remote.TasksHelper;
import com.teo.ttasks.data.remote.TokenHelper;
import com.teo.ttasks.ui.activities.sign_in.SignInPresenter;

import dagger.Module;
import dagger.Provides;

@Module
public abstract class SignInActivityModule {

    @Provides
    static SignInPresenter provideSignInPresenter(TokenHelper tokenHelper, TasksHelper tasksHelper, PrefHelper prefHelper) {
        return new SignInPresenter(tokenHelper, tasksHelper, prefHelper);
    }
}
