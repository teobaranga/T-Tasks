package com.teo.ttasks

import android.content.Context
import android.graphics.drawable.Drawable
import android.net.Uri
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
import dagger.android.AndroidInjector
import dagger.android.support.DaggerApplication
import io.fabric.sdk.android.Fabric
import io.realm.Realm
import io.realm.RealmConfiguration
import timber.log.Timber
import javax.inject.Inject

// TODO: re-schedule the CreateTask job until necessary
class TTasksApp : DaggerApplication() {

    @Inject internal lateinit var prefHelper: PrefHelper

    private lateinit var applicationComponent: ApplicationComponent

    override fun onCreate() {
        super.onCreate()

        // Apply night mode
        val nightMode = prefHelper.nightMode
        NightHelper.applyNightMode(nightMode)

        JobManager.create(this).addJobCreator(DefaultJobCreator())

        // Enable Timber
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())

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
        val realmConfiguration = RealmConfiguration.Builder()
                .deleteRealmIfMigrationNeeded()
                .initialData {
                    // Reset the ETags saved, even though this has nothing to do with Realm
                    // This is needed so that the app doesn't stop working on a schema change
                    prefHelper.deleteAllEtags()
                }
                .build()
        Realm.setDefaultConfiguration(realmConfiguration)
    }

    fun applicationComponent(): ApplicationComponent = applicationComponent

    companion object {

        // Prevent need in a singleton (global) reference to the application object.
        operator fun get(context: Context): TTasksApp = context.applicationContext as TTasksApp
    }
}
