package com.teo.ttasks;

import android.app.Application;
import android.content.Context;

import com.crashlytics.android.Crashlytics;
import com.teo.ttasks.data.local.PrefHelper;
import com.teo.ttasks.injection.component.ApplicationComponent;
import com.teo.ttasks.injection.component.DaggerApplicationComponent;
import com.teo.ttasks.injection.component.SignInComponent;
import com.teo.ttasks.injection.component.UserComponent;
import com.teo.ttasks.injection.module.ApplicationModule;
import com.teo.ttasks.injection.module.SignInModule;
import com.teo.ttasks.injection.module.UserModule;
import com.teo.ttasks.util.NightHelper;

import javax.inject.Inject;

import io.fabric.sdk.android.Fabric;
import io.realm.Realm;
import io.realm.RealmConfiguration;
import timber.log.Timber;

/**
 * @author Teo
 */
public class TTasksApp extends Application {

    @Inject PrefHelper prefHelper;

    // Initialized in onCreate. But be careful if you have ContentProviders in different processes -> their onCreate will be called before app.onCreate().
    private ApplicationComponent applicationComponent;
    private SignInComponent signInComponent;
    private UserComponent userComponent;

    // Prevent need in a singleton (global) reference to the application object.
    public static TTasksApp get(Context context) {
        return (TTasksApp) context.getApplicationContext();
    }

    @Override
    public void onCreate() {
        applicationComponent = DaggerApplicationComponent.builder()
                .applicationModule(new ApplicationModule(this))
                .build();

        applicationComponent.inject(this);

        // Apply night mode
        final String nightMode = prefHelper.getNightMode();
        NightHelper.applyNightMode(nightMode);

        super.onCreate();

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

    public SignInComponent signInComponent() {
        if (signInComponent == null)
            signInComponent = applicationComponent.plus(new SignInModule());
        return signInComponent;
    }

    public void releaseSignInComponent() {
        signInComponent = null;
    }

    public UserComponent userComponent() {
        if (userComponent == null)
            userComponent = applicationComponent.plus(new UserModule());
        return userComponent;
    }

    public void releaseUserComponent() {
        userComponent = null;
    }
}
