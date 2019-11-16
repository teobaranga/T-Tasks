package com.teo.ttasks

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.evernote.android.job.JobManager
import com.jakewharton.threetenabp.AndroidThreeTen
import com.teo.ttasks.data.local.PrefHelper
import com.teo.ttasks.injection.appModule
import com.teo.ttasks.injection.networkModule
import com.teo.ttasks.jobs.DefaultJobCreator
import com.teo.ttasks.util.DateUtils
import com.teo.ttasks.util.NightHelper
import com.teo.ttasks.util.NotificationHelper
import io.realm.Realm
import io.realm.RealmConfiguration
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.threeten.bp.ZonedDateTime
import timber.log.Timber

class TTasksApp : Application() {

    private val prefHelper: PrefHelper by inject()

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidLogger()
            // Android context
            androidContext(this@TTasksApp)
            // modules
            modules(listOf(appModule, networkModule))
        }

        DateUtils.init(this)

        AndroidThreeTen.init(this)
        ZonedDateTime.now()

        // Apply night mode
        val nightMode = prefHelper.nightMode
        NightHelper.applyNightMode(nightMode)

        JobManager.create(this).addJobCreator(DefaultJobCreator())

        // Enable Timber
        if (BuildConfig.DEBUG) {
            Timber.plant(object : Timber.DebugTree() {
                override fun createStackElementTag(element: StackTraceElement): String? {
                    return String.format(
                        "%s::%s:%s",
                        super.createStackElementTag(element),
                        element.methodName,
                        element.lineNumber
                    )
                }
            })

//            Stetho.initialize(Stetho.newInitializerBuilder(this)
//                    .enableDumpapp(Stetho.defaultDumperPluginsProvider(this))
//                    .enableWebKitInspector(RealmInspectorModulesProvider.builder(this).build())
//                    .build())
        }

        initRealmConfiguration()

        createNotificationChannel()
    }

    private fun initRealmConfiguration() {
        Realm.init(this)
        Realm.setDefaultConfiguration(
            RealmConfiguration.Builder()
                .deleteRealmIfMigrationNeeded()
                .initialData {
                    // Reset the ETags saved, even though this has nothing to do with Realm
                    // This is needed so that the app doesn't stop working on a schema change
                    prefHelper.deleteAllEtags()
                }
                .build())
    }

    private fun createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.channel_reminders)
            val description = getString(R.string.channel_reminders_description)
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(NotificationHelper.CHANNEL_REMINDERS, name, importance)
                .apply {
                    this.description = description
                }
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager!!.createNotificationChannel(channel)
        }
    }

    companion object {

        // Prevent need in a singleton (global) reference to the application object.
        operator fun get(context: Context): TTasksApp = context.applicationContext as TTasksApp
    }
}
