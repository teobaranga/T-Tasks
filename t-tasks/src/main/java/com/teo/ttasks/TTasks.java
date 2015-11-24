package com.teo.ttasks;

import android.app.Application;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.widget.ImageView;

import com.google.android.gms.plus.Plus;
import com.google.api.services.tasks.TasksScopes;
import com.mikepenz.iconics.IconicsDrawable;
import com.mikepenz.materialdrawer.util.AbstractDrawerImageLoader;
import com.mikepenz.materialdrawer.util.DrawerImageLoader;
import com.mikepenz.materialdrawer.util.DrawerUIUtils;
import com.squareup.picasso.Picasso;

import timber.log.Timber;

/**
 * @author Teo
 */
public class TTasks extends Application {

    public static final String SCOPES = "oauth2: " + TasksScopes.TASKS + " " + Plus.SCOPE_PLUS_PROFILE.toString();
    public static final String[] SCOPES2 = { TasksScopes.TASKS, Plus.SCOPE_PLUS_PROFILE.toString() };
    public static final String TASKLISTS  = "tasklists";
    public static final String TITLE  = "title";

    @Override
    public void onCreate() {
        super.onCreate();

        // Enable Timber
        if (BuildConfig.DEBUG)
            Timber.plant(new Timber.DebugTree());

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

}
