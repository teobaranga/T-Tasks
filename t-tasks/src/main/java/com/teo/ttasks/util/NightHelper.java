package com.teo.ttasks.util;

import android.content.Context;
import android.content.res.Configuration;

public class NightHelper {

    private NightHelper() { }

    public static boolean isNight(Context context) {
        return (context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
    }
}
