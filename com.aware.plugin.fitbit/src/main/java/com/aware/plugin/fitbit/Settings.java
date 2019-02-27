package com.aware.plugin.fitbit;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.widget.Toast;

import com.aware.Aware;
import com.aware.ui.AppCompatPreferenceActivity;

public class Settings extends AppCompatPreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    //Plugin settings in XML @xml/preferences_fitbit
    public static final String STATUS_PLUGIN_FITBIT = "status_plugin_fitbit";
    public static final String UNITS_PLUGIN_FITBIT = "units_plugin_fitbit";
    public static final String PLUGIN_FITBIT_FREQUENCY = "plugin_fitbit_frequency";
    public static final String FITBIT_GRANULARITY = "fitbit_granularity";
    public static final String FITBIT_HR_GRANULARITY = "fitbit_hr_granularity";
    public static final String API_KEY_PLUGIN_FITBIT = "api_key_plugin_fitbit";
    public static final String API_SECRET_PLUGIN_FITBIT = "api_secret_plugin_fitbit";
    public static final String OAUTH_TOKEN = "oauth_token";

    public static final String OAUTH_SCOPES = "oauth_scopes";
    public static final String OAUTH_VALIDITY = "oauth_validity";
    public static final String OAUTH_TOKEN_TYPE = "oauth_token_type";

    private final String FITBIT_RESET = "fitbit_reset";
    private final String FITBIT_SYNC = "fitbit_sync";

    //Plugin settings UI elements
    private CheckBoxPreference status;
    private EditTextPreference frequency, apiKey, apiSecret;
    private ListPreference units, fitbit_granularity, hr_granularity;
    private Preference clear, sync;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences_fitbit);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        status = (CheckBoxPreference) findPreference(STATUS_PLUGIN_FITBIT);
        if (Aware.getSetting(this, STATUS_PLUGIN_FITBIT).length() == 0) {
            Aware.setSetting(this, STATUS_PLUGIN_FITBIT, true); //by default, the setting is true on install
        }
        status.setChecked(Aware.getSetting(getApplicationContext(), STATUS_PLUGIN_FITBIT).equals("true"));

        units = (ListPreference) findPreference(UNITS_PLUGIN_FITBIT);
        if (Aware.getSetting(this, UNITS_PLUGIN_FITBIT).length() == 0)
            Aware.setSetting(this, UNITS_PLUGIN_FITBIT, "metric");
        units.setSummary(Aware.getSetting(this, UNITS_PLUGIN_FITBIT));

        fitbit_granularity = (ListPreference) findPreference(FITBIT_GRANULARITY);
        if (Aware.getSetting(this, FITBIT_GRANULARITY).length() == 0)
            Aware.setSetting(this, FITBIT_GRANULARITY, "15min");
        fitbit_granularity.setSummary(Aware.getSetting(this, FITBIT_GRANULARITY));

        hr_granularity = (ListPreference) findPreference(FITBIT_HR_GRANULARITY);
        if (Aware.getSetting(this, FITBIT_HR_GRANULARITY).length() == 0)
            Aware.setSetting(this, FITBIT_HR_GRANULARITY, "1sec");
        hr_granularity.setSummary(Aware.getSetting(this, FITBIT_HR_GRANULARITY));

        frequency = (EditTextPreference) findPreference(PLUGIN_FITBIT_FREQUENCY);
        if (Aware.getSetting(this, PLUGIN_FITBIT_FREQUENCY).length() == 0)
            Aware.setSetting(this, PLUGIN_FITBIT_FREQUENCY, 15);
        frequency.setText(Aware.getSetting(this, PLUGIN_FITBIT_FREQUENCY));
        frequency.setSummary("Every " + Aware.getSetting(this, PLUGIN_FITBIT_FREQUENCY) + " minute(s)");

        apiKey = (EditTextPreference) findPreference(API_KEY_PLUGIN_FITBIT);
        if (Aware.getSetting(this, API_KEY_PLUGIN_FITBIT).length() == 0)
            Aware.setSetting(this, API_KEY_PLUGIN_FITBIT, "227YG3");
        apiKey.setText(Aware.getSetting(this, API_KEY_PLUGIN_FITBIT));
        apiKey.setSummary(Aware.getSetting(this, API_KEY_PLUGIN_FITBIT));

        apiSecret = (EditTextPreference) findPreference(API_SECRET_PLUGIN_FITBIT);
        if (Aware.getSetting(this, API_SECRET_PLUGIN_FITBIT).length() == 0)
            Aware.setSetting(this, API_SECRET_PLUGIN_FITBIT, "033ed2a3710c0cde04343d073c09e378");
        apiSecret.setText(Aware.getSetting(this, API_SECRET_PLUGIN_FITBIT));
        apiSecret.setSummary(Aware.getSetting(this, API_SECRET_PLUGIN_FITBIT));

        clear = (Preference) findPreference(FITBIT_RESET);
        clear.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Toast.makeText(getApplicationContext(), "Data and account removed!", Toast.LENGTH_SHORT).show();

                Plugin.devicesPicker = null;
                Aware.setSetting(getApplicationContext(), Settings.OAUTH_TOKEN, "");
                Aware.setSetting(getApplicationContext(), Settings.OAUTH_SCOPES, "");
                Aware.setSetting(getApplicationContext(), Settings.OAUTH_VALIDITY, "");
                Aware.setSetting(getApplicationContext(), Settings.OAUTH_TOKEN_TYPE, "");
                getContentResolver().delete(Provider.Fitbit_Data.CONTENT_URI, null, null);
                getContentResolver().delete(Provider.Fitbit_Devices.CONTENT_URI, null, null);
                Plugin.fitbitAPI = null;
                Plugin.fitbitOAUTHToken = null;

                Aware.stopPlugin(getApplicationContext(), "com.aware.plugin.fitbit");

                finish();
                return true;
            }
        });

        sync = (Preference) findPreference(FITBIT_SYNC);
        sync.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent fetchdata = new Intent(getApplicationContext(), Plugin.class);
                fetchdata.setAction(Plugin.ACTION_AWARE_PLUGIN_FITBIT_SYNC);
                startService(fetchdata);

                Toast.makeText(getApplicationContext(), "Synching!", Toast.LENGTH_SHORT).show();
                return true;
            }
        });
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Preference preference = findPreference(key);
        if (preference.getKey().equals(STATUS_PLUGIN_FITBIT)) {
            Aware.setSetting(this, key, sharedPreferences.getBoolean(key, false));
            status.setChecked(sharedPreferences.getBoolean(key, false));
        }

        if (preference.getKey().equals(UNITS_PLUGIN_FITBIT)) {
            Aware.setSetting(getApplicationContext(), key, sharedPreferences.getString(key, "metric"));
            preference.setSummary(Aware.getSetting(this, UNITS_PLUGIN_FITBIT));
        }

        if (preference.getKey().equals(FITBIT_GRANULARITY)) {
            Aware.setSetting(getApplicationContext(), key, sharedPreferences.getString(key, "15min"));
            preference.setSummary(Aware.getSetting(this, FITBIT_GRANULARITY));
        }

        if (preference.getKey().equals(FITBIT_HR_GRANULARITY)) {
            Aware.setSetting(getApplicationContext(), key, sharedPreferences.getString(key, "1sec"));
            preference.setSummary(Aware.getSetting(this, FITBIT_HR_GRANULARITY));
        }

        if (preference.getKey().equals(PLUGIN_FITBIT_FREQUENCY)) {
            Aware.setSetting(getApplicationContext(), key, sharedPreferences.getString(key, "15"));
            preference.setSummary("Every " + Aware.getSetting(this, PLUGIN_FITBIT_FREQUENCY) + " minute(s)");
        }

        if (preference.getKey().equals(API_KEY_PLUGIN_FITBIT)) {
            Aware.setSetting(getApplicationContext(), key, sharedPreferences.getString(key, "227YG3"));
        }

        if (preference.getKey().equals(API_SECRET_PLUGIN_FITBIT)) {
            Aware.setSetting(getApplicationContext(), key, sharedPreferences.getString(key, "033ed2a3710c0cde04343d073c09e378"));
        }

        if (Aware.getSetting(this, STATUS_PLUGIN_FITBIT).equals("true")) {
            Aware.startPlugin(getApplicationContext(), "com.aware.plugin.fitbit");
        } else {
            Aware.stopPlugin(getApplicationContext(), "com.aware.plugin.fitbit");
        }
    }
}
