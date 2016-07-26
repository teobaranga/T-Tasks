package com.teo.ttasks.injection.module;

import android.support.annotation.NonNull;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.plus.Plus;
import com.teo.ttasks.TTasksApp;
import com.teo.ttasks.data.local.PrefHelper;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

/** It's a Dagger module that provides application level dependencies. */
@Module
public class ApplicationModule {

    private static final String SCOPE_TASKS = "https://www.googleapis.com/auth/tasks";
    public static final String SCOPE_GOOGLE_TASKS = "oauth2:https://www.googleapis.com/auth/tasks";

    @NonNull
    private final TTasksApp ttasksApp;

    public ApplicationModule(@NonNull TTasksApp ttasksApp) {
        this.ttasksApp = ttasksApp;
    }

    @Provides @Singleton
    TTasksApp provideTTasksApp() {
        return ttasksApp;
    }

    @Provides @Singleton
    PrefHelper providePrefHelper() {
        return new PrefHelper(ttasksApp);
    }

    @Provides @Singleton
    GoogleApiClient provideGoogleApiClient(TTasksApp ttasksApp) {

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestProfile()
                .requestScopes(new Scope(SCOPE_TASKS))
                .build();

        return new GoogleApiClient.Builder(ttasksApp)
                .addApi(Plus.API)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();
    }
}
