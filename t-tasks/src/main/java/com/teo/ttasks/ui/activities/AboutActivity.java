package com.teo.ttasks.ui.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.mikepenz.aboutlibraries.Libs;
import com.mikepenz.aboutlibraries.LibsBuilder;
import com.mikepenz.aboutlibraries.ui.LibsActivity;
import com.teo.ttasks.R;

public class AboutActivity extends LibsActivity {

    public static void start(Context context) {
        context.startActivity(new Intent(context, AboutActivity.class));
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {

        setIntent(new LibsBuilder()
                //Pass the fields of your application to the lib so it can find all external lib information
                .withFields(R.string.class.getFields())
                .withAboutAppName(getResources().getString(R.string.app_name))
                .withAboutVersionShownName(true)
                .withAboutDescription(getString(R.string.about_description))
                .withAboutIconShown(true)
                .withActivityStyle(Libs.ActivityStyle.LIGHT_DARK_TOOLBAR)
                .withLicenseShown(true)
                .withActivityTitle(getString(R.string.about))
                .intent(this));

        super.onCreate(savedInstanceState);
    }
}
