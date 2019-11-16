package com.teo.ttasks.util

import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate

object NightHelper {

    const val NIGHT_AUTO = "auto"
    const val NIGHT_NEVER = "never"
    const val NIGHT_ALWAYS = "always"
    const val NIGHT_SYSTEM = "system"

    fun isNight(context: Context): Boolean {
        return context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
    }

    fun applyNightMode(nightMode: String) {
        when (nightMode) {
            NIGHT_NEVER -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            NIGHT_AUTO -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY)
            NIGHT_ALWAYS -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            NIGHT_SYSTEM -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }
}
