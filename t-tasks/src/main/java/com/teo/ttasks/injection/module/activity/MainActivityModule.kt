package com.teo.ttasks.injection.module.activity

import com.teo.ttasks.UserManager
import com.teo.ttasks.api.PeopleApi
import com.teo.ttasks.data.local.PrefHelper
import com.teo.ttasks.data.remote.TasksHelper
import com.teo.ttasks.ui.activities.main.MainActivityPresenter
import dagger.Module
import dagger.Provides

@Module
class MainActivityModule {

    @Provides
    internal fun provideMainActivityPresenter(
        tasksHelper: TasksHelper,
        prefHelper: PrefHelper,
        peopleApi: PeopleApi,
        userManager: UserManager
    ): MainActivityPresenter {
        return MainActivityPresenter(tasksHelper, prefHelper, peopleApi, userManager)
    }
}
