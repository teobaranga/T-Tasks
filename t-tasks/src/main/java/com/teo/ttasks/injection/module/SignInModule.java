package com.teo.ttasks.injection.module;

import com.teo.ttasks.data.local.PrefHelper;
import com.teo.ttasks.data.remote.TasksHelper;
import com.teo.ttasks.data.remote.TokenHelper;
import com.teo.ttasks.ui.activities.sign_in.SignInPresenter;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class SignInModule {

    @Provides @Singleton
    SignInPresenter provideSignInPresenter(TokenHelper tokenHelper, TasksHelper tasksHelper, PrefHelper prefHelper) {
        return new SignInPresenter(tokenHelper, tasksHelper, prefHelper);
    }
}
