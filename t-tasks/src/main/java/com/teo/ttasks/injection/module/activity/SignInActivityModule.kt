package com.teo.ttasks.injection.module.activity

import com.teo.ttasks.data.local.PrefHelper
import com.teo.ttasks.data.remote.TasksHelper
import com.teo.ttasks.data.remote.TokenHelper
import com.teo.ttasks.ui.activities.sign_in.SignInPresenter

import dagger.Module
import dagger.Provides

@Module
class SignInActivityModule {

    @Provides
    internal fun provideSignInPresenter(tokenHelper: TokenHelper, tasksHelper: TasksHelper, prefHelper: PrefHelper): SignInPresenter {
        return SignInPresenter(tokenHelper, tasksHelper, prefHelper)
    }
}
