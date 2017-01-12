package com.aware.plugin.fitbit;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;

import com.aware.Aware;
import com.aware.Aware_Preferences;
import com.aware.ui.PermissionsHandler;
import com.aware.utils.Aware_Plugin;
import com.aware.utils.Scheduler;
import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Response;
import com.github.scribejava.core.model.Verb;
import com.github.scribejava.core.oauth.OAuth20Service;

import net.danlew.android.joda.JodaTimeAndroid;

import org.joda.time.DateTime;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class Plugin extends Aware_Plugin {

    /**
     * Broadcasted when we have new data on the database
     */
    public static final String ACTION_AWARE_PLUGIN_FITBIT = "ACTION_AWARE_PLUGIN_FITBIT";

    /**
     * Request the plugin to check for new data at Fitbit.
     * NOTE: Fitbit API limit of 150 times per hour per device
     */
    public static final String ACTION_AWARE_PLUGIN_FITBIT_SYNC = "ACTION_AWARE_PLUGIN_FITBIT_SYNC";

    private static final String SCHEDULER_PLUGIN_FITBIT = "SCHEDULER_PLUGIN_FITBIT";

    public static OAuth20Service fitbitAPI;
    public static OAuth2AccessToken fitbitOAUTHToken;

    private static final int FITBIT_NOTIFICATION_ID = 54321;

    @Override
    public void onCreate() {
        super.onCreate();

        TAG = getResources().getString(R.string.app_name);

        //Any active plugin/sensor shares its overall context using broadcasts
        CONTEXT_PRODUCER = new ContextProducer() {
            @Override
            public void onContext() {
                //Broadcast your context here
                Intent fitbitData = new Intent(ACTION_AWARE_PLUGIN_FITBIT);
                sendBroadcast(fitbitData);
            }
        };

        DATABASE_TABLES = Provider.DATABASE_TABLES;
        TABLES_FIELDS = Provider.TABLES_FIELDS;
        CONTEXT_URIS = new Uri[]{Provider.Fitbit_Data.CONTENT_URI, Provider.Fitbit_Devices.CONTENT_URI};

        Aware.startPlugin(this, "com.aware.plugin.fitbit");
    }

    //This function gets called every 5 minutes by AWARE to make sure this plugin is still running.
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        boolean permissions_ok = true;
        for (String p : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) {
                permissions_ok = false;
                break;
            }
        }

        if (permissions_ok) {
            //Check if the user has toggled the debug messages
            DEBUG = Aware.getSetting(this, Aware_Preferences.DEBUG_FLAG).equals("true");

            //Initialize our plugin's settings
            Aware.setSetting(this, Settings.STATUS_PLUGIN_FITBIT, true);

            // Set default values for the plugin
            if (Aware.getSetting(getApplicationContext(), Settings.UNITS_PLUGIN_FITBIT).length() == 0)
                Aware.setSetting(getApplicationContext(), Settings.UNITS_PLUGIN_FITBIT, "metric");

            if (Aware.getSetting(getApplicationContext(), Settings.PLUGIN_FITBIT_FREQUENCY).length() == 0)
                Aware.setSetting(getApplicationContext(), Settings.PLUGIN_FITBIT_FREQUENCY, 15);

            if (Aware.getSetting(getApplicationContext(), Settings.API_KEY_PLUGIN_FITBIT).length() == 0)
                Aware.setSetting(getApplicationContext(), Settings.API_KEY_PLUGIN_FITBIT, "227YG3");

            if (Aware.getSetting(getApplicationContext(), Settings.API_SECRET_PLUGIN_FITBIT).length() == 0)
                Aware.setSetting(getApplicationContext(), Settings.API_SECRET_PLUGIN_FITBIT, "033ed2a3710c0cde04343d073c09e378");

            if (Aware.getSetting(getApplicationContext(), Settings.OAUTH_TOKEN).length() > 0 && fitbitAPI != null && fitbitOAUTHToken != null) {

                Cursor devices = getContentResolver().query(Provider.Fitbit_Devices.CONTENT_URI, null, null, null, Provider.Fitbit_Devices.TIMESTAMP + " ASC");
                //Ask the user to pick the Fitbit they will use if not set
                if (devices == null || devices.getCount() == 0) {
                    new FitbitDevicesPicker().execute();
                }
                if (devices != null && !devices.isClosed()) devices.close();

                try {
                    Scheduler.Schedule fitbitFetcher = Scheduler.getSchedule(this, SCHEDULER_PLUGIN_FITBIT);
                    if (fitbitFetcher == null || fitbitFetcher.getInterval() != Long.valueOf(Aware.getSetting(this, Settings.PLUGIN_FITBIT_FREQUENCY))) {
                        fitbitFetcher = new Scheduler.Schedule(SCHEDULER_PLUGIN_FITBIT);
                        fitbitFetcher.setInterval(Long.valueOf(Aware.getSetting(this, Settings.PLUGIN_FITBIT_FREQUENCY)))
                                .setActionType(Scheduler.ACTION_TYPE_SERVICE)
                                .setActionClass(getPackageName() + "/" + getClass().getName())
                                .setActionIntentAction(ACTION_AWARE_PLUGIN_FITBIT_SYNC);

                        Scheduler.saveSchedule(this, fitbitFetcher);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                if (intent != null && intent.getAction() != null && intent.getAction().equalsIgnoreCase(ACTION_AWARE_PLUGIN_FITBIT_SYNC)) {
                    if (Plugin.fitbitOAUTHToken != null && Plugin.fitbitAPI != null) {
                        new FibitDataSync().execute();
                    }
                }

            } else {
                if (Aware.getSetting(this, Settings.OAUTH_TOKEN).length() == 0) {
                    Intent fitbitAuth = new Intent(this, FitbitAuth.class);
                    fitbitAuth.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, fitbitAuth, PendingIntent.FLAG_ONE_SHOT);

                    NotificationCompat.Builder notBuilder = new NotificationCompat.Builder(this);
                    notBuilder.setSmallIcon(R.drawable.ic_stat_fitbit)
                            .setContentTitle(getString(R.string.app_name))
                            .setContentText(getString(R.string.fitbit_authenticate))
                            .setAutoCancel(true)
                            .setContentIntent(pendingIntent);

                    Notification notification = notBuilder.build();
                    notification.flags |= Notification.FLAG_NO_CLEAR;

                    NotificationManager notManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                    notManager.notify(FITBIT_NOTIFICATION_ID, notification);
                } else {
                    try {
                        if (Plugin.fitbitAPI == null)
                            restoreFitbitAPI(getApplicationContext());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        } else {
            Intent permissions = new Intent(this, PermissionsHandler.class);
            permissions.putExtra(PermissionsHandler.EXTRA_REQUIRED_PERMISSIONS, REQUIRED_PERMISSIONS);
            permissions.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(permissions);
        }

        return super.onStartCommand(intent, flags, startId);
    }

    private class FibitDataSync extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {

            try {
                if (Plugin.fitbitAPI == null)
                    restoreFitbitAPI(getApplicationContext());

                String devices = fetchData(getApplicationContext(), "https://api.fitbit.com/1/user/-/devices.json");
                if (devices == null) {
                    if (Plugin.DEBUG)
                        Log.d(Plugin.TAG, "Unable to fetch user's devices status information");

                    return null;
                }

                //Get data now that we have authenticated with Fitbit
                JSONArray devices_fitbit = new JSONArray(devices);
                if (Plugin.DEBUG)
                    Log.d(Plugin.TAG, "Latest info on server (devices): " + devices_fitbit.toString(5));

                for (int i = 0; i < devices_fitbit.length(); i++) {

                    JSONObject fit = devices_fitbit.getJSONObject(i);

                    Cursor device = getContentResolver().query(Provider.Fitbit_Devices.CONTENT_URI, null, Provider.Fitbit_Devices.FITBIT_ID + " LIKE '" + fit.getString("id") + "'", null, Provider.Fitbit_Devices.TIMESTAMP + " DESC LIMIT 1");
                    if (device != null && device.moveToFirst()) {

                        JodaTimeAndroid.init(getApplicationContext());
                        DateTime localSync = DateTime.parse(device.getString(device.getColumnIndex(Provider.Fitbit_Devices.LAST_SYNC)));
                        DateTime serverSync = DateTime.parse(fit.getString("lastSyncTime"));

                        Cursor localData = getContentResolver().query(Provider.Fitbit_Data.CONTENT_URI, null, null, null, null);
                        if (!localSync.isEqual(serverSync) || (localData == null || localData.getCount() == 0)) {

                            String localSyncDate = device.getString(device.getColumnIndex(Provider.Fitbit_Devices.LAST_SYNC)).split("T")[0];
                            String serverSyncDate = fit.getString("lastSyncTime").split("T")[0];

                            String steps = fetchData(getApplicationContext(), "https://api.fitbit.com/1/user/-/activities/steps/date/" + localSyncDate + "/" + serverSyncDate + "/15min.json");
                            if (steps == null) {
                                if (Plugin.DEBUG)
                                    Log.d(Plugin.TAG, "No steps for this device.");
                            } else {
                                JSONObject steps_data = new JSONObject(steps);
                                ContentValues stepsData = new ContentValues();
                                stepsData.put(Provider.Fitbit_Data.TIMESTAMP, System.currentTimeMillis());
                                stepsData.put(Provider.Fitbit_Data.DEVICE_ID, Aware.getSetting(getApplicationContext(), Aware_Preferences.DEVICE_ID));
                                stepsData.put(Provider.Fitbit_Data.FITBIT_ID, fit.getString("id"));
                                stepsData.put(Provider.Fitbit_Data.DATA_TYPE, "steps");
                                stepsData.put(Provider.Fitbit_Data.FITBIT_JSON, steps_data.toString());
                                getContentResolver().insert(Provider.Fitbit_Data.CONTENT_URI, stepsData);

                                if (Plugin.DEBUG)
                                    Log.d(Plugin.TAG, "New steps: " + steps_data.toString(5));
                            }

                            String calories = fetchData(getApplicationContext(), "https://api.fitbit.com/1/user/-/activities/calories/date/" + localSyncDate + "/" + serverSyncDate + "/15min.json");
                            if (calories == null) {
                                if (Plugin.DEBUG)
                                    Log.d(Plugin.TAG, "No steps for this device.");
                            } else {
                                JSONObject calories_data = new JSONObject(calories);
                                ContentValues caloriesData = new ContentValues();
                                caloriesData.put(Provider.Fitbit_Data.TIMESTAMP, System.currentTimeMillis());
                                caloriesData.put(Provider.Fitbit_Data.DEVICE_ID, Aware.getSetting(getApplicationContext(), Aware_Preferences.DEVICE_ID));
                                caloriesData.put(Provider.Fitbit_Data.FITBIT_ID, fit.getString("id"));
                                caloriesData.put(Provider.Fitbit_Data.DATA_TYPE, "calories");
                                caloriesData.put(Provider.Fitbit_Data.FITBIT_JSON, calories_data.toString());
                                getContentResolver().insert(Provider.Fitbit_Data.CONTENT_URI, caloriesData);

                                if (Plugin.DEBUG)
                                    Log.d(Plugin.TAG, "New calories: " + calories_data.toString(5));
                            }

                            String heartrate = fetchData(getApplicationContext(), "https://api.fitbit.com/1/user/-/activities/heart/date/" + localSyncDate + "/" + serverSyncDate + "/1min.json");
                            if (heartrate == null) {
                                if (Plugin.DEBUG)
                                    Log.d(Plugin.TAG, "No heartrate for this device.");
                            } else {
                                JSONObject heartrate_data = new JSONObject(heartrate);
                                ContentValues heartRateData = new ContentValues();
                                heartRateData.put(Provider.Fitbit_Data.TIMESTAMP, System.currentTimeMillis());
                                heartRateData.put(Provider.Fitbit_Data.DEVICE_ID, Aware.getSetting(getApplicationContext(), Aware_Preferences.DEVICE_ID));
                                heartRateData.put(Provider.Fitbit_Data.FITBIT_ID, fit.getString("id"));
                                heartRateData.put(Provider.Fitbit_Data.DATA_TYPE, "heartrate");
                                heartRateData.put(Provider.Fitbit_Data.FITBIT_JSON, heartrate_data.toString());
                                getContentResolver().insert(Provider.Fitbit_Data.CONTENT_URI, heartRateData);

                                if (Plugin.DEBUG)
                                    Log.d(Plugin.TAG, "New heartrate: " + heartrate_data.toString(5));
                            }

                            //will have all the sleep related data
                            JSONArray sleep = new JSONArray();

                            String sleep_efficiency = fetchData(getApplicationContext(), "https://api.fitbit.com/1/user/-/sleep/efficiency/date/" + localSyncDate + "/" + serverSyncDate + ".json");
                            if (sleep_efficiency == null) {
                                if (Plugin.DEBUG)
                                    Log.d(Plugin.TAG, "No sleep efficiency for this device.");
                            } else {
                                JSONObject efficiency_data = new JSONObject(sleep_efficiency);
                                sleep.put(efficiency_data);
                            }

                            String sleep_time_in_bed = fetchData(getApplicationContext(), "https://api.fitbit.com/1/user/-/sleep/timeInBed/date/" + localSyncDate + "/" + serverSyncDate + ".json");
                            if (sleep_time_in_bed == null) {
                                if (Plugin.DEBUG)
                                    Log.d(Plugin.TAG, "No sleep time to bed for this device.");
                            } else {
                                JSONObject time_to_bed_data = new JSONObject(sleep_time_in_bed);
                                sleep.put(time_to_bed_data);
                            }

                            String sleep_minutes_awake = fetchData(getApplicationContext(), "https://api.fitbit.com/1/user/-/sleep/minutesAwake/date/" + localSyncDate + "/" + serverSyncDate + ".json");
                            if (sleep_minutes_awake == null) {
                                if (Plugin.DEBUG)
                                    Log.d(Plugin.TAG, "No sleep minutes awake for this device.");
                            } else {
                                JSONObject minutes_awake_data = new JSONObject(sleep_minutes_awake);
                                sleep.put(minutes_awake_data);
                            }

                            String sleep_minutes_to_sleep = fetchData(getApplicationContext(), "https://api.fitbit.com/1/user/-/sleep/minutesToFallAsleep/date/" + localSyncDate + "/" + serverSyncDate + ".json");
                            if (sleep_minutes_to_sleep == null) {
                                if (Plugin.DEBUG)
                                    Log.d(Plugin.TAG, "No sleep minutes to sleep for this device.");
                            } else {
                                JSONObject minutes_to_sleep_data = new JSONObject(sleep_minutes_to_sleep);
                                sleep.put(minutes_to_sleep_data);
                            }

                            String sleep_awake_count = fetchData(getApplicationContext(), "https://api.fitbit.com/1/user/-/sleep/awakeCount/date/" + localSyncDate + "/" + serverSyncDate + ".json");
                            if (sleep_awake_count == null) {
                                if (Plugin.DEBUG)
                                    Log.d(Plugin.TAG, "No sleep awake count for this device.");
                            } else {
                                JSONObject awake_count_data = new JSONObject(sleep_awake_count);
                                sleep.put(awake_count_data);
                            }

                            if (sleep.length() > 0) {
                                ContentValues sleepData = new ContentValues();
                                sleepData.put(Provider.Fitbit_Data.TIMESTAMP, System.currentTimeMillis());
                                sleepData.put(Provider.Fitbit_Data.DEVICE_ID, Aware.getSetting(getApplicationContext(), Aware_Preferences.DEVICE_ID));
                                sleepData.put(Provider.Fitbit_Data.FITBIT_ID, fit.getString("id"));
                                sleepData.put(Provider.Fitbit_Data.DATA_TYPE, "sleep");
                                sleepData.put(Provider.Fitbit_Data.FITBIT_JSON, sleep.toString());
                                getContentResolver().insert(Provider.Fitbit_Data.CONTENT_URI, sleepData);

                                if (Plugin.DEBUG)
                                    Log.d(Plugin.TAG, "New sleep: " + sleep.toString(5));
                            }

                            //Save the latest sync time. We want to check later how often the fitbits actually synched.
                            ContentValues latestData = new ContentValues();
                            latestData.put(Provider.Fitbit_Devices.TIMESTAMP, System.currentTimeMillis());
                            latestData.put(Provider.Fitbit_Devices.DEVICE_ID, Aware.getSetting(getApplicationContext(), Aware_Preferences.DEVICE_ID));
                            latestData.put(Provider.Fitbit_Devices.FITBIT_ID, fit.getString("id"));
                            latestData.put(Provider.Fitbit_Devices.FITBIT_BATTERY, fit.getString("battery"));
                            latestData.put(Provider.Fitbit_Devices.FITBIT_VERSION, fit.getString("deviceVersion"));
                            latestData.put(Provider.Fitbit_Devices.FITBIT_MAC, fit.optString("mac", ""));
                            latestData.put(Provider.Fitbit_Devices.LAST_SYNC, fit.getString("lastSyncTime"));
                            getContentResolver().insert(Provider.Fitbit_Devices.CONTENT_URI, latestData);

                            if (CONTEXT_PRODUCER != null)
                                CONTEXT_PRODUCER.onContext();
                        }
                        if (localData != null && ! localData.isClosed()) localData.close();
                    }
                    if (device != null && !device.isClosed()) device.close();
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

            return null;
        }
    }

    /**
     * Ask the user to choose which Fitbit will be used with the plugin
     */
    public class FitbitDevicesPicker extends AsyncTask<Void, Void, Boolean> {
        @Override
        protected Boolean doInBackground(Void... params) {

            if (Plugin.fitbitAPI == null || Plugin.fitbitOAUTHToken == null)
                return false;

            OAuthRequest request = new OAuthRequest(Verb.GET, "https://api.fitbit.com/1/user/-/devices.json", Plugin.fitbitAPI);
            request.addHeader("Authorization",
                    " " + Aware.getSetting(getApplicationContext(), Settings.OAUTH_TOKEN_TYPE) +
                    " " + Aware.getSetting(getApplicationContext(), Settings.OAUTH_TOKEN));

            Plugin.fitbitAPI.signRequest(Plugin.fitbitOAUTHToken, request);
            Response response = request.send();

            if (response.isSuccessful()) {
                try {
                    Intent devicePicker = new Intent(getApplicationContext(), DevicePicker.class);
                    devicePicker.putExtra(DevicePicker.DEVICES_JSON, response.getBody());
                    devicePicker.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(devicePicker);
                } catch (IOException e) {
                    return false;
                }
                return true;
            }
            return false;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);

            if (!result) {
                Toast.makeText(getApplicationContext(), "Failed to load available devices. Try authenticating again.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Get data from a resource URL endpoint
     *
     * @param context
     * @param resource_url
     * @return
     */
    public String fetchData(Context context, String resource_url) {
        if (Plugin.fitbitAPI == null) {
            try {
                restoreFitbitAPI(context);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        OAuthRequest request = new OAuthRequest(Verb.GET, resource_url, Plugin.fitbitAPI);
        request.addHeader("Authorization", " " + Aware.getSetting(context, Settings.OAUTH_TOKEN_TYPE) + " " + Aware.getSetting(context, Settings.OAUTH_TOKEN));

        String metric = "";
        if (Aware.getSetting(context, Settings.UNITS_PLUGIN_FITBIT).equalsIgnoreCase("metric"))
            metric = "Metric";
        if (Aware.getSetting(context, Settings.UNITS_PLUGIN_FITBIT).equalsIgnoreCase("imperial"))
            metric = "en_US";
        request.addHeader("Accept-Language", metric);

        Plugin.fitbitAPI.signRequest(Plugin.fitbitOAUTHToken, request);
        Response response = request.send();

        if (response.isSuccessful()) {
            try {
                return response.getBody();
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }
        return null;
    }

    /**
     * Restore connection to the Fitbit API endpoints
     *
     * @param context
     * @throws JSONException
     */
    public void restoreFitbitAPI(Context context) throws JSONException {
        Plugin.fitbitOAUTHToken = new OAuth2AccessToken(Aware.getSetting(context, Settings.OAUTH_TOKEN),
                Aware.getSetting(context, Settings.OAUTH_TOKEN_TYPE),
                Integer.valueOf(Aware.getSetting(context, Settings.OAUTH_VALIDITY)),
                "null",
                Aware.getSetting(context, Settings.OAUTH_SCOPES),
                "null");

        JSONObject scopes = new JSONObject(Aware.getSetting(context, Settings.OAUTH_SCOPES));

        String scopes_str = (scopes.getBoolean("activity")) ? "activity" : "";
        scopes_str += (scopes.getBoolean("heartrate")) ? " heartrate" : "";
        scopes_str += (scopes.getBoolean("sleep")) ? " sleep" : "";
        scopes_str += (scopes.getBoolean("settings")) ? " settings" : "";

        Plugin.fitbitAPI = new ServiceBuilder()
                .apiKey(Aware.getSetting(context, Settings.API_KEY_PLUGIN_FITBIT))
                .scope(scopes_str)
                .responseType("token")
                .callback("fitbit://logincallback")
                .apiSecret(Aware.getSetting(context, Settings.API_SECRET_PLUGIN_FITBIT))
                .build(FitbitAPI.instance());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        Aware.setSetting(this, Settings.STATUS_PLUGIN_FITBIT, false);
        Scheduler.removeSchedule(this, SCHEDULER_PLUGIN_FITBIT);

        NotificationManager notManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notManager.cancel(FITBIT_NOTIFICATION_ID);

        //Stop AWARE
        Aware.stopAWARE(this);
    }
}
