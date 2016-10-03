package com.teo.ttasks.util;

import android.content.Context;
import android.content.res.Configuration;
import android.support.v7.app.AppCompatDelegate;

public class NightHelper {

    public static final String NIGHT_AUTO = "auto";
    public static final String NIGHT_NEVER = "never";
    public static final String NIGHT_ALWAYS = "always";

    private NightHelper() { }

    public static boolean isNight(Context context) {
        return (context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
    }

    public static void applyNightMode(String nightMode) {
        switch (nightMode) {
            case NIGHT_NEVER:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case NIGHT_AUTO:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO);
                break;
            case NIGHT_ALWAYS:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
        }
    }
}
