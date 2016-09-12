package com.teo.ttasks.ui.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.PreferenceFragment;
import android.preference.RingtonePreference;
import android.support.v7.app.AppCompatActivity;

import com.teo.ttasks.R;

public class SettingsActivity extends AppCompatActivity {

    public static void start(Context context) {
        context.startActivity(new Intent(context, SettingsActivity.class));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Display the fragment as the main content.
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();
    }

    public static class SettingsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

        private static final String REMINDER_SOUND = "reminder_sound";
        private static final String REMINDER_COLOR = "reminder_color";

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            // do nothing yet
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            // Load the preferences from an XML resource
            addPreferencesFromResource(R.xml.preferences);

            final SharedPreferences sharedPreferences = getPreferenceManager().getSharedPreferences();

            final RingtonePreference reminderSound = ((RingtonePreference) findPreference(REMINDER_SOUND));
            final String defaultSound = sharedPreferences.getString(REMINDER_SOUND, getString(R.string.default_reminder_sound));
            setReminderSound(reminderSound, defaultSound);

            reminderSound.setOnPreferenceChangeListener((preference, newValue) -> {
                setReminderSound(((RingtonePreference) preference), ((String) newValue));
                return true;
            });

            final ListPreference reminderColor = (ListPreference) findPreference(REMINDER_COLOR);
            final String defaultColor = sharedPreferences.getString(REMINDER_COLOR, getString(R.string.default_led_color));
            setColor(reminderColor, defaultColor);

            reminderColor.setOnPreferenceChangeListener((preference, newValue) -> {
                setColor(((ListPreference) preference), ((String) newValue));
                return true;
            });

            // set texts correctly
            onSharedPreferenceChanged(null, "");
        }

        @Override
        public void onStart() {
            super.onStart();
            getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onStop() {
            getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
            super.onStop();
        }

        private void setReminderSound(RingtonePreference preference, String sound) {
            if (sound == null || sound.isEmpty()) {
                preference.setSummary(R.string.pref_ringtone_silent);
            } else {
                final Ringtone ringtone = RingtoneManager.getRingtone(getActivity(), Uri.parse(sound));
                if (ringtone != null) {
                    preference.setSummary(ringtone.getTitle(getActivity()));
                }
            }
        }

        private void setColor(ListPreference preference, String colorString) {
            if (colorString == null || colorString.isEmpty()) {
                preference.setSummary(getString(R.string.none));
            } else {
                char ch = colorString.charAt(0);
                final String color = Character.isTitleCase(ch) ? colorString : Character.toTitleCase(ch) + colorString.substring(1);
                preference.setSummary(color);
            }
        }
    }
}
