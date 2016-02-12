package com.teo.ttasks;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.multidex.MultiDexApplication;
import android.widget.ImageView;

import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.mikepenz.iconics.IconicsDrawable;
import com.mikepenz.materialdrawer.util.AbstractDrawerImageLoader;
import com.mikepenz.materialdrawer.util.DrawerImageLoader;
import com.mikepenz.materialdrawer.util.DrawerUIUtils;
import com.squareup.picasso.Picasso;
import com.teo.ttasks.injection.component.ApplicationComponent;
import com.teo.ttasks.injection.component.DaggerApplicationComponent;
import com.teo.ttasks.injection.component.TasksApiComponent;
import com.teo.ttasks.injection.module.ApplicationModule;
import com.teo.ttasks.injection.module.TasksApiModule;

import io.realm.Realm;
import io.realm.RealmConfiguration;
import timber.log.Timber;

/**
 * @author Teo
 */
public class TTasksApp extends MultiDexApplication {

    @SuppressWarnings("NullableProblems")
    // Initialized in onCreate. But be careful if you have ContentProviders in different processes -> their onCreate will be called before app.onCreate().
    @NonNull
    private ApplicationComponent applicationComponent;

    @Nullable
    private TasksApiComponent mTasksApiComponent;

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

        applicationComponent = DaggerApplicationComponent.builder()
                .applicationModule(new ApplicationModule(this))
                .build();

        applicationComponent.inject(this);

        initRealmConfiguration();

        //initialize and create the image loader logic
        DrawerImageLoader.init(new AbstractDrawerImageLoader() {
            @Override
            public void set(ImageView imageView, Uri uri, Drawable placeholder) {
                Picasso.with(imageView.getContext()).load(uri).placeholder(placeholder).into(imageView);
            }

            @Override
            public void cancel(ImageView imageView) {
                Picasso.with(imageView.getContext()).cancelRequest(imageView);
            }

            @Override
            public Drawable placeholder(Context ctx, String tag) {
                //define different placeholders for different imageView targets
                //default tags are accessible via the DrawerImageLoader.Tags
                //custom ones can be checked via string. see the CustomUrlBasePrimaryDrawerItem LINE 111
                if (DrawerImageLoader.Tags.PROFILE.name().equals(tag)) {
                    return DrawerUIUtils.getPlaceHolder(ctx);
                } else if (DrawerImageLoader.Tags.ACCOUNT_HEADER.name().equals(tag)) {
                    return new IconicsDrawable(ctx).iconText(" ").backgroundColorRes(com.mikepenz.materialdrawer.R.color.primary).sizeDp(56);
                } else if ("customUrlItem".equals(tag)) {
                    return new IconicsDrawable(ctx).iconText(" ").backgroundColorRes(R.color.md_red_500).sizeDp(56);
                }

                return super.placeholder(ctx, tag);
            }
        });
    }

    private void initRealmConfiguration() {
        RealmConfiguration realmConfiguration = new RealmConfiguration.Builder(this)
                .deleteRealmIfMigrationNeeded()
                .build();
        Realm.setDefaultConfiguration(realmConfiguration);
    }

    @NonNull
    public ApplicationComponent applicationComponent() {
        return applicationComponent;
    }

    @NonNull
    public TasksApiComponent createTasksApiComponent(@NonNull GoogleAccountCredential credential) {
        mTasksApiComponent = applicationComponent.plus(new TasksApiModule(credential));
        return mTasksApiComponent;
    }

    @Nullable
    public TasksApiComponent tasksApiComponent() {
        return mTasksApiComponent;
    }

    // TODO: 2015-12-25 call this at logout
    public void releaseTasksApiComponent() {
        mTasksApiComponent = null;
    }

}
