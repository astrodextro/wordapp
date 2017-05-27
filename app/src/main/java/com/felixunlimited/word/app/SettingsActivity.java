/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.felixunlimited.word.app;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.widget.Toast;

import java.io.File;

/**
 * A {@link PreferenceActivity} that presents a set of application settings.
 * <p>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
public class SettingsActivity extends PreferenceActivity
        implements Preference.OnPreferenceChangeListener {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Add 'general' preferences, defined in the XML file
        addPreferencesFromResource(R.xml.pref_general);
        //Utility.chooseTheme(this);

        // For all preferences, attach an OnPreferenceChangeListener so the UI summary can be
        // updated when the preference changes. /storage/emulated/0/Android/data/com.felixunlimited.word.app/files
        bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_dir_chooser_key)));
        bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_decl_interval_key)));
//        bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_themes_key)));
    }

    /**
     * Attaches a listener so the summary is always updated with the preference value.
     * Also fires the listener once, to initialize the summary (so it shows up before the value
     * is changed.)
     */
    private void bindPreferenceSummaryToValue(Preference preference) {
        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(this);

        Object value = PreferenceManager
                .getDefaultSharedPreferences(preference.getContext())
                .getString(preference.getKey(), "");
        String stringValue = value.toString();

        if (preference instanceof ListPreference) {
            // For list preferences, look up the correct display value in
            // the preference's 'entries' list (since they have separate labels/values).
            ListPreference listPreference = (ListPreference) preference;
            int prefIndex = listPreference.findIndexOfValue(stringValue);

            if (prefIndex >= 0) {
                preference.setSummary(listPreference.getEntries()[prefIndex]);
            }

        } else {
            // For other preferences, set the summary to the value's simple string representation.
            preference.setSummary(stringValue);
        }
        // Trigger the listener immediately with the preference's
        // current value.
//        onPreferenceChange(preference,
//                PreferenceManager
//                        .getDefaultSharedPreferences(preference.getContext())
//                        .getString(preference.getKey(), ""));
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object value) {
        String stringValue = value.toString();
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        final Context context = getApplicationContext();
        File internalDir = context.getFilesDir();
        File externalDir = context.getExternalFilesDir(null);
        Toast.makeText(context, "internal free space: "+Utility.readableFileSize(internalDir.getFreeSpace()), Toast.LENGTH_LONG).show();
        Toast.makeText(context, "external free space: "+Utility.readableFileSize(externalDir.getFreeSpace()), Toast.LENGTH_LONG).show();
        Toast.makeText(context, "internal size: "+Utility.readableFileSize(Utility.getFolderSize(internalDir)), Toast.LENGTH_LONG).show();
        Toast.makeText(context, "external size: "+Utility.readableFileSize(Utility.getFolderSize(externalDir)), Toast.LENGTH_LONG).show();

        if (preference instanceof ListPreference) {
            // For list preferences, look up the correct display value in
            // the preference's 'entries' list (since they have separate labels/values).
            ListPreference listPreference = (ListPreference) preference;
            int prefIndex = listPreference.findIndexOfValue(stringValue);

            if (prefIndex >= 0) {
                if (listPreference.getKey().equals(getString(R.string.pref_dir_chooser_key))) {
                    if (listPreference.getValue().equals(getString(R.string.pref_dir_chooser_internal))) {
                        if (internalDir.getFreeSpace() < 10000000) {
                            Toast.makeText(context, "Insufficient space in internal memory. Switching to external...", Toast.LENGTH_SHORT).show();
                            listPreference.getEntries()[prefIndex] = sharedPreferences.getString(context.getString(R.string.pref_dir_chooser_external), null);
                        }
                        else {
                            preference.setSummary(listPreference.getEntries()[prefIndex]);
                            if (Utility.getFolderSize(externalDir) > internalDir.getFreeSpace())
                                new Utility.MoveFilesTask (context).execute(externalDir, internalDir);
                        }
                    }
                    else {
                        if (externalDir.getFreeSpace() < 10000000) {
                            Toast.makeText(context, "Insufficient space in external memory. Switching to internal...", Toast.LENGTH_SHORT).show();
                            listPreference.getEntries()[prefIndex] = sharedPreferences.getString(context.getString(R.string.pref_dir_chooser_internal), null);
                        }
                        else if (Utility.isExternalStorageWritable()) {
                            Toast.makeText(context, "External memory not writable. Switching to internal...", Toast.LENGTH_SHORT).show();
                            listPreference.getEntries()[prefIndex] = sharedPreferences.getString(context.getString(R.string.pref_dir_chooser_internal), null);
                        }
                        else {
                            preference.setSummary(listPreference.getEntries()[prefIndex]);
                            if (Utility.getFolderSize(internalDir) > externalDir.getFreeSpace())
                                new Utility.MoveFilesTask (context).execute(internalDir, externalDir);
                        }
                    }
                }
            }

        } else {
            // For other preferences, set the summary to the value's simple string representation.
            preference.setSummary(stringValue);
        }
//        try {
//            stringValue = StorageUtil.getRemovebleSDCardPath();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
        return true;
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public Intent getParentActivityIntent() {
        return super.getParentActivityIntent().addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    }
}

/*
package com.felixunlimited.word.app;

        import android.annotation.TargetApi;
        import android.content.Context;
        import android.content.res.Configuration;
        import android.media.Ringtone;
        import android.media.RingtoneManager;
        import android.net.Uri;
        import android.os.Build;
        import android.os.Bundle;
        import android.preference.ListPreference;
        import android.preference.Preference;
        import android.preference.PreferenceActivity;
        import android.preference.PreferenceFragment;
        import android.preference.PreferenceManager;
        import android.preference.RingtonePreference;
        import android.text.TextUtils;

        import java.util.List;


*/
/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p/>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 *//*

public class SettingsActivity1 extends PreferenceActivity {


    */
/**
     * {@inheritDoc}
     *//*

    @Override
    public boolean onIsMultiPane() {
        return isXLargeTablet(this);
    }

    */
/**
     * Helper method to determine if the device has an extra-large screen. For
     * example, 10" tablets are extra-large.
     *//*

    private static boolean isXLargeTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
    }


    */
/**
     * {@inheritDoc}
     *//*

    @Override
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.pref_headers, target);
    }

    */
/**
     * A preference value change listener that updates the preference's summary
     * to reflect its new value.
     *//*

    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            String stringValue = value.toString();

            if (preference instanceof ListPreference) {
                // For list preferences, look up the correct display value in
                // the preference's 'entries' list.
                ListPreference listPreference = (ListPreference) preference;
                int index = listPreference.findIndexOfValue(stringValue);

                // Set the summary to reflect the new value.
                preference.setSummary(
                        index >= p0
                                ? listPreference.getEntries()[index]
                                : null);

            } else if (preference instanceof RingtonePreference) {
                // For ringtone preferences, look up the correct display value
                // using RingtoneManager.
                if (TextUtils.isEmpty(stringValue)) {
                    // Empty values correspond to 'silent' (no ringtone).
                    preference.setSummary(R.string.pref_ringtone_silent);

                } else {
                    Ringtone ringtone = RingtoneManager.getRingtone(
                            preference.getContext(), Uri.parse(stringValue));

                    if (ringtone == null) {
                        // Clear the summary if there was a lookup error.
                        preference.setSummary(null);
                    } else {
                        // Set the summary to reflect the new ringtone display
                        // name.
                        String name = ringtone.getTitle(preference.getContext());
                        preference.setSummary(name);
                    }
                }

            } else {
                // For all other preferences, set the summary to the value's
                // simple string representation.
                preference.setSummary(stringValue);
            }
            return true;
        }
    };

    */
/**
     * Binds a preference's summary to its value. More specifically, when the
     * preference's value is changed, its summary (line of text below the
     * preference title) is updated to reflect the value. The summary is also
     * immediately updated upon calling this method. The exact display format is
     * dependent on the type of preference.
     *
     * @see #sBindPreferenceSummaryToValueListener
     *//*

    private static void bindPreferenceSummaryToValue(Preference preference) {
        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

        // Trigger the listener immediately with the preference's
        // current value.
        sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                PreferenceManager
                        .getDefaultSharedPreferences(preference.getContext())
                        .getString(preference.getKey(), ""));
    }

    */
/**
     * This fragment shows general preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     *//*

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class GeneralPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_general);

            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
            // to their values. When their values change, their summaries are
            // updated to reflect the new value, per the Android Design
            // guidelines.
            bindPreferenceSummaryToValue(findPreference("example_text"));
            bindPreferenceSummaryToValue(findPreference("example_list"));
        }
    }

    */
/**
     * This fragment shows notification preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     *//*

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class NotificationPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_notification);

            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
            // to their values. When their values change, their summaries are
            // updated to reflect the new value, per the Android Design
            // guidelines.
            bindPreferenceSummaryToValue(findPreference("notifications_new_message_ringtone"));
        }
    }

    */
/**
     * This fragment shows data and sync preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     *//*

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class DataSyncPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_data_sync);

            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
            // to their values. When their values change, their summaries are
            // updated to reflect the new value, per the Android Design
            // guidelines.
            bindPreferenceSummaryToValue(findPreference("sync_frequency"));
        }
    }
}
*/
