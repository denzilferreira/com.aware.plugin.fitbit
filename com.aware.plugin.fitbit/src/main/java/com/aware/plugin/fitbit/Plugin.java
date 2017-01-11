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
import android.support.customtabs.CustomTabsIntent;
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

import org.joda.time.DateTime;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Hashtable;

public class Plugin extends Aware_Plugin {

    public static final String ACTION_AWARE_PLUGIN_FITBIT = "ACTION_AWARE_PLUGIN_FITBIT";

    private static final String SCHEDULER_PLUGIN_FITBIT = "SCHEDULER_PLUGIN_FITBIT";
    private static final String ACTION_AWARE_PLUGIN_FITBIT_SYNC = "ACTION_AWARE_PLUGIN_FITBIT_SYNC";

    public static OAuth20Service fitbitAPI;
    public static OAuth2AccessToken fitbitOAUTHToken;

    private static final int FITBIT_NOTIFICATION_ID = 54321;

    private NotificationManager notManager;

    @Override
    public void onCreate() {
        super.onCreate();

        TAG = getResources().getString(R.string.app_name);

        notManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

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
        if (intent != null && intent.getAction() != null && intent.getAction().equalsIgnoreCase(ACTION_AWARE_PLUGIN_FITBIT_SYNC)) {
            if (Plugin.fitbitOAUTHToken == null || Plugin.fitbitAPI == null) {
                try {
                    FitbitAPI.restoreFitbitAPI(getApplicationContext());

                    //Get data now that we have authenticated with Fitbit
                    JSONArray devices_fitbit = new JSONArray(FitbitAPI.fetchData(getApplicationContext(), "https://api.fitbit.com/1/user/-/devices.json"));
                    for (int i = 0; i < devices_fitbit.length(); i++) {
                        JSONObject fit = devices_fitbit.getJSONObject(i);
                        Cursor device = getContentResolver().query(Provider.Fitbit_Devices.CONTENT_URI, null, null, null, null);
                        if (device != null && device.moveToFirst()) {
                            if (fit.getString("id").equalsIgnoreCase(device.getString(device.getColumnIndex(Provider.Fitbit_Devices.FITBIT_ID)))) {

                                DateTime localSync = DateTime.parse(device.getString(device.getColumnIndex(Provider.Fitbit_Devices.LAST_SYNC)));
                                DateTime serverSync = DateTime.parse(fit.getString("lastSyncTime"));

                                //new data exists
                                if ( !localSync.isEqual(serverSync)) {
                                    String localSyncDate = device.getString(device.getColumnIndex(Provider.Fitbit_Devices.LAST_SYNC)).split("T")[0];
                                    String serverSyncDate = fit.getString("lastSyncTime").split("T")[0];

                                    //Fetch latest data we don't have yet
                                    JSONArray activities = new JSONArray(FitbitAPI.fetchData(getApplicationContext(), "https://api.fitbit.com/1/user/-/activities/date/" + localSyncDate + "/" + serverSyncDate + ".json"));
                                    JSONArray hr = new JSONArray(FitbitAPI.fetchData(getApplicationContext(), "https://api.fitbit.com/1/user/-/heart/date/" + localSyncDate + "/" + serverSyncDate + ".json"));
                                    JSONArray sleep = new JSONArray(FitbitAPI.fetchData(getApplicationContext(), "https://api.fitbit.com/1/user/-/sleep/date/" + localSyncDate + "/" + serverSyncDate + ".json"));

                                    ContentValues activityData = new ContentValues();
                                    activityData.put(Provider.Fitbit_Data.TIMESTAMP, System.currentTimeMillis());
                                    activityData.put(Provider.Fitbit_Data.DEVICE_ID, Aware.getSetting(getApplicationContext(), Aware_Preferences.DEVICE_ID));
                                    activityData.put(Provider.Fitbit_Data.DATA_TYPE, "activities");
                                    activityData.put(Provider.Fitbit_Data.FITBIT_JSON, activities.toString());
                                    getContentResolver().insert(Provider.Fitbit_Data.CONTENT_URI, activityData);

                                    if (Plugin.DEBUG)
                                        Log.d(Plugin.TAG, "New activities: " + activities.toString(5));

                                    ContentValues heartRateData = new ContentValues();
                                    activityData.put(Provider.Fitbit_Data.TIMESTAMP, System.currentTimeMillis());
                                    activityData.put(Provider.Fitbit_Data.DEVICE_ID, Aware.getSetting(getApplicationContext(), Aware_Preferences.DEVICE_ID));
                                    activityData.put(Provider.Fitbit_Data.DATA_TYPE, "heartrate");
                                    activityData.put(Provider.Fitbit_Data.FITBIT_JSON, hr.toString());
                                    getContentResolver().insert(Provider.Fitbit_Data.CONTENT_URI, heartRateData);

                                    if (Plugin.DEBUG)
                                        Log.d(Plugin.TAG, "New heartrate: " + hr.toString(5));

                                    ContentValues sleepData = new ContentValues();
                                    activityData.put(Provider.Fitbit_Data.TIMESTAMP, System.currentTimeMillis());
                                    activityData.put(Provider.Fitbit_Data.DEVICE_ID, Aware.getSetting(getApplicationContext(), Aware_Preferences.DEVICE_ID));
                                    activityData.put(Provider.Fitbit_Data.DATA_TYPE, "sleep");
                                    activityData.put(Provider.Fitbit_Data.FITBIT_JSON, sleep.toString());
                                    getContentResolver().insert(Provider.Fitbit_Data.CONTENT_URI, sleepData);

                                    if (Plugin.DEBUG)
                                        Log.d(Plugin.TAG, "New sleep: " + sleep.toString(5));

                                    //Update to the latest sync time
                                    ContentValues latestData = new ContentValues();
                                    latestData.put(Provider.Fitbit_Devices.FITBIT_BATTERY, fit.getString("battery"));
                                    latestData.put(Provider.Fitbit_Devices.LAST_SYNC, fit.getString("lastSyncTime"));

                                    getContentResolver().update(
                                            Provider.Fitbit_Devices.CONTENT_URI, latestData,
                                            Provider.Fitbit_Devices._ID + "=" + device.getInt(device.getColumnIndex(Provider.Fitbit_Devices._ID)),
                                            null);

                                    CONTEXT_PRODUCER.onContext();
                                }
                            }
                        }
                        if (device != null && ! device.isClosed()) device.close();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }

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
                Aware.setSetting(getApplicationContext(), Settings.PLUGIN_FITBIT_FREQUENCY, 5);

            if (Aware.getSetting(getApplicationContext(), Settings.API_KEY_PLUGIN_FITBIT).length() == 0)
                Aware.setSetting(getApplicationContext(), Settings.API_KEY_PLUGIN_FITBIT, "227YG3");

            if (Aware.getSetting(getApplicationContext(), Settings.API_SECRET_PLUGIN_FITBIT).length() == 0)
                Aware.setSetting(getApplicationContext(), Settings.API_SECRET_PLUGIN_FITBIT, "033ed2a3710c0cde04343d073c09e378");

            if (Aware.getSetting(getApplicationContext(), Settings.OAUTH_TOKEN).length() > 0 && fitbitAPI != null && fitbitOAUTHToken != null) {
                Cursor devices = getContentResolver().query(Provider.Fitbit_Devices.CONTENT_URI, null, null, null, Provider.Fitbit_Devices.TIMESTAMP + " ASC");
                if (devices == null || devices.getCount() == 0) {
                    //Ask the user to pick the Fitbit they will use
                    new Plugin.FitbitDevicesPicker(getApplicationContext()).execute();
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
                    notManager.notify(FITBIT_NOTIFICATION_ID, notification);
                } else {
                    try {
                        FitbitAPI.restoreFitbitAPI(getApplicationContext());
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



    /**
     * Ask the user to choose which Fitbit will be used with the plugin
     */
    public static class FitbitDevicesPicker extends AsyncTask<Void, Void, Boolean> {
        private Context mContext;

        FitbitDevicesPicker(Context context) {
            mContext = context;
        }

        @Override
        protected Boolean doInBackground(Void... params) {

            if (Aware.getSetting(mContext, Settings.OAUTH_TOKEN).length() == 0 || Plugin.fitbitAPI == null || Plugin.fitbitOAUTHToken == null) return false;

            OAuthRequest request = new OAuthRequest(Verb.GET, "https://api.fitbit.com/1/user/-/devices.json", Plugin.fitbitAPI);
            request.addHeader("Authorization",
                    " " + Aware.getSetting(mContext, Settings.OAUTH_TOKEN_TYPE) +
                            " " + Aware.getSetting(mContext, Settings.OAUTH_TOKEN));

            Plugin.fitbitAPI.signRequest(Plugin.fitbitOAUTHToken, request);
            Response response = request.send();

            if (response.isSuccessful()) {
                try {
                    Intent devicePicker = new Intent(mContext, DevicePicker.class);
                    devicePicker.putExtra(DevicePicker.DEVICES_JSON, response.getBody());
                    devicePicker.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    mContext.startActivity(devicePicker);
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
                Toast.makeText(mContext, "Failed to load available devices. Try authenticating again.", Toast.LENGTH_SHORT).show();
                try {
                    FitbitAPI.authorizeFitbit(mContext);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        Aware.setSetting(this, Settings.STATUS_PLUGIN_FITBIT, false);

        //Stop AWARE
        Aware.stopAWARE(this);
    }
}
