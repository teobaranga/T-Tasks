package com.teo.ttasks.injection.module

import android.content.Context
import com.teo.ttasks.TTasksApp
import com.teo.ttasks.UserManager
import com.teo.ttasks.data.local.PrefHelper
import com.teo.ttasks.data.local.WidgetHelper
import com.teo.ttasks.receivers.NetworkInfoReceiver
import com.teo.ttasks.util.NotificationHelper
import dagger.Module
import dagger.Provides
import io.mockk.mockk
import javax.inject.Singleton

@Module
class TestApplicationModule {

    @Provides
    internal fun provideContext(application: TTasksApp): Context = application.applicationContext

    @Provides
    @Singleton
    internal fun providePrefHelper(context: Context): PrefHelper = PrefHelper(context)

    @Provides
    @Singleton
    internal fun provideWidgetHelper(): WidgetHelper = mockk()

    @Provides
    @Singleton
    internal fun provideNotificationHelper(): NotificationHelper = mockk()

    @Provides
    @Singleton
    internal fun provideNetworkInfoReceiver(): NetworkInfoReceiver = mockk()

    @Provides
    @Singleton
    fun provideUserManager(): UserManager = mockk()
}
