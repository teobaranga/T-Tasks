package com.teo.ttasks.ui.activities

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.preference.ListPreference
import android.preference.PreferenceFragment
import android.preference.RingtonePreference
import android.support.v7.app.AppCompatActivity
import com.teo.ttasks.R
import com.teo.ttasks.util.NightHelper
import timber.log.Timber

class SettingsActivity : AppCompatActivity() {

    private var nightModeChanged = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // Display the fragment as the main content.
        fragmentManager.beginTransaction()
                .replace(android.R.id.content, SettingsFragment())
                .commit()

        if (savedInstanceState != null) {
            nightModeChanged = savedInstanceState.getBoolean(ARG_NIGHT_MODE_CHANGED, false)
            Timber.d("night mode changed %s", nightModeChanged)
            if (nightModeChanged)
                setResult(Activity.RESULT_OK)
        }
    }

    public override fun onSaveInstanceState(outState: Bundle) {
        Timber.d("saving")
        outState.putBoolean(ARG_NIGHT_MODE_CHANGED, nightModeChanged)
        super.onSaveInstanceState(outState)
    }

    fun setNightModeChanged(nightModeChanged: Boolean) {
        this.nightModeChanged = nightModeChanged
    }

    class SettingsFragment : PreferenceFragment(), SharedPreferences.OnSharedPreferenceChangeListener {

        override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String) {
            // do nothing yet
        }

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            // Load the preferences from an XML resource
            addPreferencesFromResource(R.xml.preferences)

            val sharedPreferences = preferenceManager.sharedPreferences

            val nightMode = findPreference(NIGHT_MODE) as ListPreference
            val initialNightMode = sharedPreferences.getString(NIGHT_MODE, getString(R.string.default_night_mode))
            setNightMode(nightMode, initialNightMode)

            nightMode.setOnPreferenceChangeListener { preference, newValue ->
                setNightMode(preference as ListPreference, newValue as String)
                val oldValue = preference.value
                NightHelper.applyNightMode(newValue)
                val activity = activity as SettingsActivity
                if (initialNightMode != newValue) {
                    Timber.d("changed")
                    activity.setNightModeChanged(true)
                } else {
                    Timber.d("not changed")
                    activity.setNightModeChanged(false)
                }
                if (oldValue != newValue)
                    activity.recreate()
                true
            }


            val reminderSound = findPreference(REMINDER_SOUND) as RingtonePreference
            val initialSound = sharedPreferences.getString(REMINDER_SOUND, getString(R.string.default_reminder_sound))
            setReminderSound(reminderSound, initialSound)

            reminderSound.setOnPreferenceChangeListener { preference, newValue ->
                setReminderSound(preference as RingtonePreference, newValue as String)
                true
            }

            val reminderColor = findPreference(REMINDER_COLOR) as ListPreference
            val initialColor = sharedPreferences.getString(REMINDER_COLOR, getString(R.string.default_led_color))
            setColor(reminderColor, initialColor)

            reminderColor.setOnPreferenceChangeListener { preference, newValue ->
                setColor(preference as ListPreference, newValue as String)
                true
            }

            // set texts correctly
            onSharedPreferenceChanged(null, "")
        }

        override fun onStart() {
            super.onStart()
            preferenceManager.sharedPreferences.registerOnSharedPreferenceChangeListener(this)
        }

        override fun onStop() {
            preferenceManager.sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
            super.onStop()
        }

        private fun setNightMode(preference: ListPreference, nightMode: String) {
            preference.summary = Character.toTitleCase(nightMode[0]) + nightMode.substring(1)
        }

        private fun setReminderSound(preference: RingtonePreference, sound: String?) {
            if (sound == null || sound.isBlank()) {
                preference.setSummary(R.string.pref_ringtone_silent)
            } else {
                RingtoneManager.getRingtone(activity, Uri.parse(sound))?.let { ringtone ->
                    preference.summary = ringtone.getTitle(activity)
                }
            }
        }

        private fun setColor(preference: ListPreference, colorString: String?) {
            if (colorString == null || colorString.isBlank()) {
                preference.summary = getString(R.string.none)
            } else {
                val ch = colorString[0]
                val color = if (Character.isTitleCase(ch)) colorString else Character.toTitleCase(ch) + colorString.substring(1)
                preference.summary = color
            }
        }

        companion object {
            private const val NIGHT_MODE = "night_mode"
            private const val REMINDER_SOUND = "reminder_sound"
            private const val REMINDER_COLOR = "reminder_color"
        }
    }

    companion object {
        private const val ARG_NIGHT_MODE_CHANGED = "nightMode"

        fun startForResult(activity: AppCompatActivity, requestCode: Int) {
            activity.startActivityForResult(Intent(activity, SettingsActivity::class.java), requestCode)
        }
    }
}
