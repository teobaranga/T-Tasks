package com.teo.ttasks

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.widget.ImageView
import com.crashlytics.android.Crashlytics
import com.evernote.android.job.JobManager
import com.mikepenz.materialdrawer.util.AbstractDrawerImageLoader
import com.mikepenz.materialdrawer.util.DrawerImageLoader
import com.squareup.picasso.Picasso
import com.teo.ttasks.data.local.PrefHelper
import com.teo.ttasks.injection.component.ApplicationComponent
import com.teo.ttasks.injection.component.DaggerApplicationComponent
import com.teo.ttasks.jobs.DefaultJobCreator
import com.teo.ttasks.util.NightHelper
import com.teo.ttasks.util.NotificationHelper
import dagger.android.AndroidInjector
import dagger.android.support.DaggerApplication
import io.fabric.sdk.android.Fabric
import io.realm.Realm
import io.realm.RealmConfiguration
import timber.log.Timber
import javax.inject.Inject

class TTasksApp : DaggerApplication() {

    @Inject
    internal lateinit var prefHelper: PrefHelper

    private lateinit var applicationComponent: ApplicationComponent

    fun applicationComponent() = applicationComponent

    override fun onCreate() {
        super.onCreate()

        // Apply night mode
        val nightMode = prefHelper.nightMode
        NightHelper.applyNightMode(nightMode)

        JobManager.create(this).addJobCreator(DefaultJobCreator())

        // Enable Timber
        if (BuildConfig.DEBUG) {
            Timber.plant(object : Timber.DebugTree() {
                override fun createStackElementTag(element: StackTraceElement): String? {
                    return String.format("%s::%s:%s",
                            super.createStackElementTag(element),
                            element.methodName,
                            element.lineNumber)
                }
            })

//            Stetho.initialize(Stetho.newInitializerBuilder(this)
//                    .enableDumpapp(Stetho.defaultDumperPluginsProvider(this))
//                    .enableWebKitInspector(RealmInspectorModulesProvider.builder(this).build())
//                    .build())
        } else {
            Fabric.with(this, Crashlytics())
        }

        initRealmConfiguration()

        //initialize and create the image loader logic
        DrawerImageLoader.init(object : AbstractDrawerImageLoader() {
            override fun set(imageView: ImageView?, uri: Uri?, placeholder: Drawable?, tag: String?) {
                var requestCreator = Picasso.get().load(uri)
                placeholder?.let { requestCreator = requestCreator.placeholder(it) }
                requestCreator.into(imageView)
            }

            override fun cancel(imageView: ImageView?) {
                imageView?.let { Picasso.get().cancelRequest(it) }
            }
        })

        createNotificationChannel()
    }

    override fun applicationInjector(): AndroidInjector<out TTasksApp> {
        val injector = DaggerApplicationComponent
                .builder()
                .create(this)
        applicationComponent = injector as ApplicationComponent
        return injector
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
