package com.teo.ttasks;

import android.content.Context;
import android.support.multidex.MultiDex;

import com.crashlytics.android.Crashlytics;
import com.teo.ttasks.data.local.PrefHelper;
import com.teo.ttasks.injection.component.ApplicationComponent;
import com.teo.ttasks.injection.component.DaggerApplicationComponent;
import com.teo.ttasks.util.NightHelper;

import javax.inject.Inject;

import dagger.android.AndroidInjector;
import dagger.android.support.DaggerApplication;
import io.fabric.sdk.android.Fabric;
import io.realm.Realm;
import io.realm.RealmConfiguration;
import timber.log.Timber;

/**
 * @author Teo
 */
public class TTasksApp extends DaggerApplication {

    @Inject PrefHelper prefHelper;

    private ApplicationComponent applicationComponent;

    // Prevent need in a singleton (global) reference to the application object.
    public static TTasksApp get(Context context) {
        return (TTasksApp) context.getApplicationContext();
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // Apply night mode
        final String nightMode = prefHelper.getNightMode();
        NightHelper.applyNightMode(nightMode);

        // Enable Timber
        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());

//            Stetho.initialize(
//                    Stetho.newInitializerBuilder(this)
//                            .enableDumpapp(Stetho.defaultDumperPluginsProvider(this))
//                            .enableWebKitInspector(RealmInspectorModulesProvider.builder(this).build())
//                            .build());
        } else {
            Fabric.with(this, new Crashlytics());
        }

        initRealmConfiguration();
    }

    @Override
    protected AndroidInjector<? extends DaggerApplication> applicationInjector() {
        final AndroidInjector<TTasksApp> injector = DaggerApplicationComponent
                .builder()
                .create(this);
        applicationComponent = (ApplicationComponent) injector;
        return injector;
    }

    private void initRealmConfiguration() {
        Realm.init(this);
        RealmConfiguration realmConfiguration = new RealmConfiguration.Builder()
                .deleteRealmIfMigrationNeeded()
                .initialData(realm -> {
                    // Reset the ETags saved, even though this has nothing to do with Realm
                    // This is needed so that the app doesn't stop working on a schema change
                    prefHelper.deleteAllEtags();
                })
                .build();
        Realm.setDefaultConfiguration(realmConfiguration);
    }

    public ApplicationComponent applicationComponent() {
        return applicationComponent;
    }
}
