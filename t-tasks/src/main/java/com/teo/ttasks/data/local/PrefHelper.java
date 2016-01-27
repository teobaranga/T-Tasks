package com.teo.ttasks.data.local;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class PrefHelper {

    private static final String PREF_USER_EMAIL = "email";

    public static boolean isUserPresent(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return preferences.getString(PREF_USER_EMAIL, null) != null;
    }

    public static void setUser(Context context, String email) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        preferences.edit().putString(PREF_USER_EMAIL, email).apply();
    }

    public static void clearUser(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        preferences.edit().remove(PREF_USER_EMAIL).apply();
    }
}
