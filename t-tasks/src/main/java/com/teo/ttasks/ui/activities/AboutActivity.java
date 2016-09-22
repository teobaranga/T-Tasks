package com.teo.ttasks.ui.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.res.ResourcesCompat;

import com.mikepenz.aboutlibraries.Libs;
import com.mikepenz.aboutlibraries.LibsBuilder;
import com.mikepenz.aboutlibraries.ui.LibsActivity;
import com.mikepenz.aboutlibraries.util.Colors;
import com.teo.ttasks.R;
import com.teo.ttasks.util.NightHelper;

public class AboutActivity extends LibsActivity {

    public static void start(Context context) {
        context.startActivity(new Intent(context, AboutActivity.class));
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {

        final boolean night = NightHelper.isNight(this);

        Colors colors = new Colors(
                ResourcesCompat.getColor(getResources(), night ? R.color.md_grey_850 : R.color.colorPrimary, null),
                ResourcesCompat.getColor(getResources(), night ? R.color.md_grey_900 : R.color.colorPrimaryDark, null)
        );

        setIntent(new LibsBuilder()
                //Pass the fields of your application to the lib so it can find all external lib information
                .withFields(R.string.class.getFields())
                .withAboutAppName(getResources().getString(R.string.app_name))
                .withAboutVersionShownName(true)
                .withAboutDescription(getString(R.string.about_description))
                .withAboutIconShown(true)
                .withActivityStyle(night ? Libs.ActivityStyle.DARK : Libs.ActivityStyle.LIGHT_DARK_TOOLBAR)
                .withActivityColor(colors)
                .withLicenseShown(true)
                .withActivityTitle(getString(R.string.about))
                .intent(this));

        super.onCreate(savedInstanceState);
    }
}
