package com.teo.ttasks;

import android.os.Bundle;

import com.mikepenz.aboutlibraries.Libs;
import com.mikepenz.aboutlibraries.LibsBuilder;
import com.mikepenz.aboutlibraries.ui.LibsActivity;

public class AboutActivity extends LibsActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {

        setIntent(new LibsBuilder()
                //Pass the fields of your application to the lib so it can find all external lib information
                .withFields(R.string.class.getFields())
                .withAboutAppName(getResources().getString(R.string.app_name))
                .withAboutVersionShownName(true)
                .withAboutDescription("Description")
                .withAboutIconShown(true)
                .withActivityStyle(Libs.ActivityStyle.LIGHT_DARK_TOOLBAR)
                .withLicenseShown(true)
                .withVersionShown(true)
                .withActivityTitle("About")
                .intent(this));

        super.onCreate(savedInstanceState);
    }
}