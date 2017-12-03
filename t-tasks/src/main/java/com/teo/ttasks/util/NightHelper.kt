package com.teo.ttasks.util

import android.content.Context
import android.content.res.Configuration
import android.support.v7.app.AppCompatDelegate

object NightHelper {

    val NIGHT_AUTO = "auto"
    val NIGHT_NEVER = "never"
    val NIGHT_ALWAYS = "always"

    fun isNight(context: Context): Boolean {
        return context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
    }

    fun applyNightMode(nightMode: String) {
        when (nightMode) {
            NIGHT_NEVER -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            NIGHT_AUTO -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO)
            NIGHT_ALWAYS -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        }
    }
}
