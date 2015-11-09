package com.teo.ttasks;

import android.app.Application;

import com.google.android.gms.plus.Plus;
import com.google.api.services.tasks.Tasks;
import com.google.api.services.tasks.TasksScopes;

import timber.log.Timber;

/**
 * @author Teo
 */
public class TTasks extends Application {

    public static final String Scopes  = "oauth2: " + TasksScopes.TASKS + " " + Plus.SCOPE_PLUS_PROFILE.toString();
    public static final String TASKLISTS  = "tasklists";
    public static final String TITLE  = "title";

    public static Tasks service = null;

    @Override
    public void onCreate() {
        super.onCreate();

        // Enable Timber
        if (BuildConfig.DEBUG)
            Timber.plant(new Timber.DebugTree());
    }

}
