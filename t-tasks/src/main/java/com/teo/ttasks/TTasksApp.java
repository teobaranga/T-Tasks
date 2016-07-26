package com.teo.ttasks;

import android.app.Application;
import android.content.Context;
import android.support.annotation.NonNull;

import com.teo.ttasks.injection.component.ApplicationComponent;
import com.teo.ttasks.injection.component.DaggerApplicationComponent;
import com.teo.ttasks.injection.component.SignInComponent;
import com.teo.ttasks.injection.component.UserComponent;
import com.teo.ttasks.injection.module.ApplicationModule;
import com.teo.ttasks.injection.module.SignInModule;
import com.teo.ttasks.injection.module.UserModule;

import io.realm.Realm;
import io.realm.RealmConfiguration;
import timber.log.Timber;

/**
 * @author Teo
 */
public class TTasksApp extends Application {

    // Initialized in onCreate. But be careful if you have ContentProviders in different processes -> their onCreate will be called before app.onCreate().
    @SuppressWarnings("NullableProblems") @NonNull
    private ApplicationComponent mApplicationComponent;

    private SignInComponent mSignInComponent;
    private UserComponent mUserComponent;

    // Prevent need in a singleton (global) reference to the application object.
    @NonNull
    public static TTasksApp get(@NonNull Context context) {
        return (TTasksApp) context.getApplicationContext();
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // Enable Timber
        if (BuildConfig.DEBUG)
            Timber.plant(new Timber.DebugTree());

        mApplicationComponent = DaggerApplicationComponent.builder()
                .applicationModule(new ApplicationModule(this))
                .build();

        mApplicationComponent.inject(this);

        initRealmConfiguration();
    }

    private void initRealmConfiguration() {
        RealmConfiguration realmConfiguration = new RealmConfiguration.Builder(this)
                .deleteRealmIfMigrationNeeded()
                .build();
        Realm.setDefaultConfiguration(realmConfiguration);
    }

    @NonNull
    public ApplicationComponent applicationComponent() {
        return mApplicationComponent;
    }

    public SignInComponent signInComponent() {
        if (mSignInComponent == null)
            mSignInComponent = mApplicationComponent.plus(new SignInModule());
        return mSignInComponent;
    }

    public void releaseSignInComponent() {
        mSignInComponent = null;
    }

    public UserComponent userComponent() {
        if (mUserComponent == null)
            mUserComponent = mApplicationComponent.plus(new UserModule());
        return mUserComponent;
    }

    public void releaseUserComponent() {
        mUserComponent = null;
    }
}
