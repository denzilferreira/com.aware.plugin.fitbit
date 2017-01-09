package com.aware.plugin.fitbit;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.support.customtabs.CustomTabsIntent;
import android.util.Log;
import android.widget.Toast;

import com.aware.Aware;
import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Response;
import com.github.scribejava.core.model.Verb;
import com.github.scribejava.core.oauth.OAuth20Service;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class Settings extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    //Plugin settings in XML @xml/preferences
    public static final String STATUS_PLUGIN_FITBIT = "status_plugin_fitbit";
    public static final String PLUGIN_FITBIT_ACTIVITY = "plugin_fitbit_activity";
    public static final String PLUGIN_FITBIT_HEART_RATE = "plugin_fitbit_heart_rate";
    public static final String PLUGIN_FITBIT_SLEEP = "plugin_fitbit_sleep";
    public static final String UNITS_PLUGIN_FITBIT = "units_plugin_fitbit";
    public static final String PLUGIN_FITBIT_FREQUENCY = "plugin_fitbit_frequency";
    public static final String API_KEY_PLUGIN_FITBIT = "api_key_plugin_fitbit";
    public static final String API_SECRET_PLUGIN_FITBIT = "api_secret_plugin_fitbit";

    public static final String OAUTH_TOKEN = "oauth_token";
    public static final String OAUTH_SCOPES = "oauth_scopes";
    public static final String OAUTH_VALIDITY = "oauth_validity";
    public static final String OAUTH_TOKEN_TYPE = "oauth_token_type";

    //Plugin settings UI elements
    private static CheckBoxPreference status, activity, heartrate, sleep;
    private static EditTextPreference frequency, apiKey, apiSecret;
    private static ListPreference units;

    private JSONObject settingsScopes = new JSONObject();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.registerOnSharedPreferenceChangeListener(this);

        /**
         * To delete at the end
         */
        Intent aware = new Intent(this, Aware.class);
        startService(aware);
        Aware.startPlugin(this, "com.aware.plugin.fitbit");
        /**
         *
         */
    }

    @Override
    protected void onResume() {
        super.onResume();

        status = (CheckBoxPreference) findPreference(STATUS_PLUGIN_FITBIT);
        if (Aware.getSetting(this, STATUS_PLUGIN_FITBIT).length() == 0) {
            Aware.setSetting(this, STATUS_PLUGIN_FITBIT, true); //by default, the setting is true on install
        }
        status.setChecked(Aware.getSetting(getApplicationContext(), STATUS_PLUGIN_FITBIT).equals("true"));

        activity = (CheckBoxPreference) findPreference(PLUGIN_FITBIT_ACTIVITY);
        if (Aware.getSetting(this, PLUGIN_FITBIT_ACTIVITY).length() == 0) {
            Aware.setSetting(this, PLUGIN_FITBIT_ACTIVITY, false);
        }
        activity.setChecked(Aware.getSetting(getApplicationContext(), STATUS_PLUGIN_FITBIT).equals("true"));

        try {
            settingsScopes.put("activity", activity.isChecked());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        heartrate = (CheckBoxPreference) findPreference(PLUGIN_FITBIT_HEART_RATE);
        if (Aware.getSetting(this, PLUGIN_FITBIT_HEART_RATE).length() == 0) {
            Aware.setSetting(this, PLUGIN_FITBIT_HEART_RATE, false);
        }
        heartrate.setChecked(Aware.getSetting(getApplicationContext(), PLUGIN_FITBIT_HEART_RATE).equals("true"));

        try {
            settingsScopes.put("heartrate", heartrate.isChecked());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        sleep = (CheckBoxPreference) findPreference(PLUGIN_FITBIT_SLEEP);
        if (Aware.getSetting(this, PLUGIN_FITBIT_SLEEP).length() == 0) {
            Aware.setSetting(this, PLUGIN_FITBIT_SLEEP, false);
        }
        sleep.setChecked(Aware.getSetting(getApplicationContext(), PLUGIN_FITBIT_SLEEP).equals("true"));
        try {
            settingsScopes.put("sleep", sleep.isChecked());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        units = (ListPreference) findPreference(UNITS_PLUGIN_FITBIT);
        if (Aware.getSetting(this, UNITS_PLUGIN_FITBIT).length() == 0)
            Aware.setSetting(this, UNITS_PLUGIN_FITBIT, "metric");
        units.setSummary(Aware.getSetting(this, UNITS_PLUGIN_FITBIT));

        frequency = (EditTextPreference) findPreference(PLUGIN_FITBIT_FREQUENCY);
        if (Aware.getSetting(this, PLUGIN_FITBIT_FREQUENCY).length() == 0)
            Aware.setSetting(this, PLUGIN_FITBIT_FREQUENCY, 5);
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
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Preference preference = findPreference(key);
        if (preference.getKey().equals(STATUS_PLUGIN_FITBIT)) {
            Aware.setSetting(this, key, sharedPreferences.getBoolean(key, false));
            status.setChecked(sharedPreferences.getBoolean(key, false));
        }

        if (preference.getKey().equals(PLUGIN_FITBIT_ACTIVITY)) {
            Aware.setSetting(getApplicationContext(), key, sharedPreferences.getBoolean(key, false));
            activity.setChecked(sharedPreferences.getBoolean(key, false));

            try {
                settingsScopes.put("activity", activity.isChecked());
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        if (preference.getKey().equals(PLUGIN_FITBIT_HEART_RATE)) {
            Aware.setSetting(getApplicationContext(), key, sharedPreferences.getBoolean(key, false));
            heartrate.setChecked(sharedPreferences.getBoolean(key, false));

            try {
                settingsScopes.put("heartrate", heartrate.isChecked());
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        if (preference.getKey().equals(PLUGIN_FITBIT_SLEEP)) {
            Aware.setSetting(getApplicationContext(), key, sharedPreferences.getBoolean(key, false));
            sleep.setChecked(sharedPreferences.getBoolean(key, false));

            try {
                settingsScopes.put("sleep", sleep.isChecked());
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        if (preference.getKey().equals(UNITS_PLUGIN_FITBIT)) {
            Aware.setSetting(getApplicationContext(), key, sharedPreferences.getString(key, "metric"));
            preference.setSummary(Aware.getSetting(this, UNITS_PLUGIN_FITBIT));
        }

        if (preference.getKey().equals(PLUGIN_FITBIT_FREQUENCY)) {
            Aware.setSetting(getApplicationContext(), key, sharedPreferences.getString(key, "5"));
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
            if (Aware.getSetting(this, OAUTH_TOKEN).length() == 0 || !isScopesMatch()) {
                Aware.setSetting(this, OAUTH_SCOPES, settingsScopes.toString());
                try {
                    authorizeFitbit(settingsScopes);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        } else {
            Aware.stopPlugin(getApplicationContext(), "com.aware.plugin.fitbit");
        }
    }

    private boolean isScopesMatch() {
        try {
            JSONObject saved = new JSONObject(Aware.getSetting(this, OAUTH_SCOPES));
            if (settingsScopes.getBoolean("activity") != saved.getBoolean("activity")
                    || settingsScopes.getBoolean("heartrate") != saved.getBoolean("heartrate")
                    || settingsScopes.getBoolean("sleep") != saved.getBoolean("sleep")) {
                // we need to re-authorize
                return false;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return true;
    }

    public void authorizeFitbit(JSONObject scopesNeeded) throws JSONException {
        String scopes = "";
        if (scopesNeeded.getBoolean("activity")) scopes += "activity ";
        if (scopesNeeded.getBoolean("heartrate")) scopes += "heartrate ";
        if (scopesNeeded.getBoolean("sleep")) scopes += "sleep ";
        scopes += "settings";

        Plugin.fitbitAPI = new ServiceBuilder()
                .apiKey(Aware.getSetting(this, Settings.API_KEY_PLUGIN_FITBIT))
                .scope(scopes)
                .responseType("token")
                .callback("fitbit://logincallback")
                .apiSecret(Aware.getSetting(this, Settings.API_SECRET_PLUGIN_FITBIT))
                .build(FitbitAPI.instance());

        CustomTabsIntent customTabsIntent = new CustomTabsIntent.Builder().build();
        customTabsIntent.launchUrl(this, Uri.parse(Plugin.fitbitAPI.getAuthorizationUrl()));
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        if (intent.getData() != null) {
            Uri URL_Fragment = intent.getData();
            String URL_Fragment_String = URL_Fragment.toString();

            Log.d(Plugin.TAG, "ANSWER:" + URL_Fragment_String);

            // Retrieve information about access token.
            String access_Token = URL_Fragment_String.substring(URL_Fragment_String.indexOf("access_token=") + 13, URL_Fragment_String.indexOf("&user_id"));
            String data_scope = URL_Fragment_String.substring(URL_Fragment_String.indexOf("scope=") + 6, URL_Fragment_String.indexOf("&token_type"));
            String token_Type = URL_Fragment_String.substring(URL_Fragment_String.indexOf("token_type=") + 11, URL_Fragment_String.indexOf("&expires_in"));
            int expires_In = Integer.parseInt(URL_Fragment_String.substring(URL_Fragment_String.indexOf("expires_in=") + 11, URL_Fragment_String.length()));

            Aware.setSetting(this, Settings.OAUTH_TOKEN, access_Token);

            try {
                JSONObject data_scopes = new JSONObject();
                data_scopes.put("activity", data_scope.toLowerCase().contains("activity".toLowerCase()));
                data_scopes.put("heartrate", data_scope.toLowerCase().contains("heartrate".toLowerCase()));
                data_scopes.put("sleep", data_scope.toLowerCase().contains("sleep".toLowerCase()));

                settingsScopes = data_scopes;

                Aware.setSetting(this, Settings.OAUTH_SCOPES, settingsScopes.toString());

            } catch (JSONException e) {
                e.printStackTrace();
            }

            Aware.setSetting(this, Settings.OAUTH_TOKEN_TYPE, token_Type);
            Aware.setSetting(this, Settings.OAUTH_VALIDITY, expires_In);

            Plugin.fitbitOAUTHToken = new OAuth2AccessToken(Aware.getSetting(this, OAUTH_TOKEN),
                    Aware.getSetting(this, OAUTH_TOKEN_TYPE),
                    Integer.valueOf(Aware.getSetting(this, OAUTH_VALIDITY)),
                    "null",
                    Aware.getSetting(this, OAUTH_SCOPES),
                    "null");

            new getFitbitDevices().execute();
        }
    }

    private class getFitbitDevices extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            OAuthRequest request = new OAuthRequest(Verb.GET, "https://api.fitbit.com/1/user/-/devices.json", Plugin.fitbitAPI);
            request.addHeader("Authorization",
                    " " + Aware.getSetting(getApplicationContext(), Settings.OAUTH_TOKEN_TYPE) +
                    " " + Aware.getSetting(getApplicationContext(), Settings.OAUTH_TOKEN));

            Plugin.fitbitAPI.signRequest(Plugin.fitbitOAUTHToken, request);
            Response response = request.send();

            try {
                Intent devicePicker = new Intent(getApplicationContext(), DevicePicker.class);
                devicePicker.putExtra(DevicePicker.DEVICES_JSON, response.getBody().toString());
                startActivity(devicePicker);
            } catch (IOException e) {
                Toast.makeText(getApplicationContext(), "Failed to load available devices. Try again.", Toast.LENGTH_SHORT).show();
            }
            return null;
        }
    }
}
