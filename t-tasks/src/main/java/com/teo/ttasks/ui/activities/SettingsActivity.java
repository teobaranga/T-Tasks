package com.teo.ttasks.ui.activities;

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
import com.teo.ttasks.util.NightHelper;

import timber.log.Timber;

public class SettingsActivity extends AppCompatActivity {

    private static final String ARG_NIGHT_MODE_CHANGED = "nightMode";

    private boolean nightModeChanged;

    public static void startForResult(AppCompatActivity activity, int requestCode) {
        activity.startActivityForResult(new Intent(activity, SettingsActivity.class), requestCode);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Display the fragment as the main content.
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();

        if (savedInstanceState != null) {
            nightModeChanged = savedInstanceState.getBoolean(ARG_NIGHT_MODE_CHANGED, false);
            Timber.d("night mode changed %s", nightModeChanged);
            if (nightModeChanged)
                setResult(RESULT_OK);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        Timber.d("saving");
        outState.putBoolean(ARG_NIGHT_MODE_CHANGED, nightModeChanged);
        super.onSaveInstanceState(outState);
    }

    public void setNightModeChanged(boolean nightModeChanged) {
        this.nightModeChanged = nightModeChanged;
    }

    public static class SettingsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

        private static final String NIGHT_MODE = "night_mode";
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

            final ListPreference nightMode = ((ListPreference) findPreference(NIGHT_MODE));
            final String initialNightMode = sharedPreferences.getString(NIGHT_MODE, getString(R.string.default_night_mode));
            setNightMode(nightMode, initialNightMode);

            nightMode.setOnPreferenceChangeListener((preference, newValue) -> {
                setNightMode(((ListPreference) preference), ((String) newValue));
                final String oldValue = ((ListPreference) preference).getValue();
                NightHelper.applyNightMode(((String) newValue));
                final SettingsActivity activity = ((SettingsActivity) getActivity());
                if (!initialNightMode.equals(newValue)) {
                    Timber.d("changed");
                    activity.setNightModeChanged(true);
                } else {
                    Timber.d("not changed");
                    activity.setNightModeChanged(false);
                }
                if (!oldValue.equals(newValue))
                    activity.recreate();
                return true;
            });


            final RingtonePreference reminderSound = ((RingtonePreference) findPreference(REMINDER_SOUND));
            final String initialSound = sharedPreferences.getString(REMINDER_SOUND, getString(R.string.default_reminder_sound));
            setReminderSound(reminderSound, initialSound);

            reminderSound.setOnPreferenceChangeListener((preference, newValue) -> {
                setReminderSound(((RingtonePreference) preference), ((String) newValue));
                return true;
            });

            final ListPreference reminderColor = (ListPreference) findPreference(REMINDER_COLOR);
            final String initialColor = sharedPreferences.getString(REMINDER_COLOR, getString(R.string.default_led_color));
            setColor(reminderColor, initialColor);

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

        private void setNightMode(ListPreference preference, String nightMode) {
            if (nightMode != null && !nightMode.isEmpty()) {
                preference.setSummary(Character.toTitleCase(nightMode.charAt(0)) + nightMode.substring(1));
            }
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
