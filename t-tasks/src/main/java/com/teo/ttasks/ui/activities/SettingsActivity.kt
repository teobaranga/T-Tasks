package com.teo.ttasks.ui.activities

import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings.System.DEFAULT_NOTIFICATION_URI
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.teo.ttasks.R
import com.teo.ttasks.data.local.PrefHelper
import org.koin.core.KoinComponent
import org.koin.core.inject

class SettingsActivity : AppCompatActivity() {

    companion object {

        fun start(context: Context) {
            context.startActivity(Intent(context, SettingsActivity::class.java))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // Display the fragment as the main content.
        supportFragmentManager.beginTransaction()
                .replace(android.R.id.content, SettingsFragment())
                .commit()
    }

    class SettingsFragment : PreferenceFragmentCompat(), KoinComponent {

        companion object {
            private const val REQUEST_CODE_ALERT_RINGTONE = 0
        }

        private val prefHelper: PrefHelper by inject()

        private lateinit var prefKeyRingtone: String

        private var currentRingtoneUri: Uri? = prefHelper.notificationSound

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.preferences, rootKey)
        }

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            prefKeyRingtone = getString(R.string.pref_notification_sound_key)

            updateRingtonePrefSummary()
        }

        override fun onPreferenceTreeClick(preference: Preference): Boolean {
            return when (preference.key) {
                prefKeyRingtone -> {
                    val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                        putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
                        putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                        putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
                        putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, DEFAULT_NOTIFICATION_URI)
                        putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, currentRingtoneUri)
                    }

                    startActivityForResult(intent, REQUEST_CODE_ALERT_RINGTONE)
                    true
                }
                else -> {
                    super.onPreferenceTreeClick(preference)
                }
            }
        }

        override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
            if (requestCode == REQUEST_CODE_ALERT_RINGTONE && data != null) {
                currentRingtoneUri = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
                prefHelper.notificationSound = currentRingtoneUri
                updateRingtonePrefSummary()
            } else {
                super.onActivityResult(requestCode, resultCode, data)
            }
        }

        private fun updateRingtonePrefSummary() {
            val soundPref = findPreference<Preference>(prefKeyRingtone)!!
            val sound = prefHelper.notificationSound

            if (sound == null) {
                soundPref.summary = getString(R.string.pref_notification_sound_silent)
            } else {
                RingtoneManager.getRingtone(activity, sound)?.let { ringtone ->
                    soundPref.summary = ringtone.getTitle(activity)
                }
            }
        }
    }
}
