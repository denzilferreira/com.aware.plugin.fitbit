package com.aware.plugin.fitbit;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.support.v4.content.ContextCompat;

import com.aware.Aware;
import com.aware.Aware_Preferences;
import com.aware.ui.PermissionsHandler;
import com.aware.utils.Aware_Plugin;
import com.aware.utils.Scheduler;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.oauth.OAuth20Service;

import org.json.JSONException;

public class Plugin extends Aware_Plugin {

    public static final String ACTION_AWARE_PLUGIN_FITBIT = "ACTION_AWARE_PLUGIN_FITBIT";

    private static final String SCHEDULER_PLUGIN_FITBIT = "SCHEDULER_PLUGIN_FITBIT";
    private static final String ACTION_AWARE_PLUGIN_FITBIT_SYNC = "ACTION_AWARE_PLUGIN_FITBIT_SYNC";

    public static OAuth20Service fitbitAPI;
    public static OAuth2AccessToken fitbitOAUTHToken;

    @Override
    public void onCreate() {
        super.onCreate();

        TAG = getResources().getString(R.string.app_name);

        //Any active plugin/sensor shares its overall context using broadcasts
        CONTEXT_PRODUCER = new ContextProducer() {
            @Override
            public void onContext() {
                //Broadcast your context here
                //TODO notify other apps that you have collected more fitbit data
            }
        };


        //To sync data to the server, you'll need to set this variables from your ContentProvider
        DATABASE_TABLES = Provider.DATABASE_TABLES;
        TABLES_FIELDS = Provider.TABLES_FIELDS;
        CONTEXT_URIS = new Uri[]{Provider.Fitbit_Data.CONTENT_URI, Provider.Fitbit_Devices.CONTENT_URI}; //this syncs dummy Fitbit_Data to server

        //Activate plugin -- do this ALWAYS as the last thing (this will restart your own plugin and apply the settings)
        Aware.startPlugin(this, "com.aware.plugin.fitbit");
    }

    //This function gets called every 5 minutes by AWARE to make sure this plugin is still running.
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null && intent.getAction().equalsIgnoreCase(ACTION_AWARE_PLUGIN_FITBIT_SYNC)) {
            //fetch fitbit data

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
            if (Aware.getSetting(getApplicationContext(), Settings.PLUGIN_FITBIT_ACTIVITY).length() == 0)
                Aware.setSetting(getApplicationContext(), Settings.PLUGIN_FITBIT_ACTIVITY, false);

            if (Aware.getSetting(getApplicationContext(), Settings.PLUGIN_FITBIT_HEART_RATE).length() == 0)
                Aware.setSetting(getApplicationContext(), Settings.PLUGIN_FITBIT_HEART_RATE, false);

            if (Aware.getSetting(getApplicationContext(), Settings.PLUGIN_FITBIT_SLEEP).length() == 0)
                Aware.setSetting(getApplicationContext(), Settings.PLUGIN_FITBIT_SLEEP, false);

            if (Aware.getSetting(getApplicationContext(), Settings.UNITS_PLUGIN_FITBIT).length() == 0)
                Aware.setSetting(getApplicationContext(), Settings.UNITS_PLUGIN_FITBIT, "metric");

            if (Aware.getSetting(getApplicationContext(), Settings.PLUGIN_FITBIT_FREQUENCY).length() == 0)
                Aware.setSetting(getApplicationContext(), Settings.PLUGIN_FITBIT_FREQUENCY, 5);

            if (Aware.getSetting(getApplicationContext(), Settings.API_KEY_PLUGIN_FITBIT).length() == 0)
                Aware.setSetting(getApplicationContext(), Settings.API_KEY_PLUGIN_FITBIT, "227YG3");

            if (Aware.getSetting(getApplicationContext(), Settings.API_SECRET_PLUGIN_FITBIT).length() == 0)
                Aware.setSetting(getApplicationContext(), Settings.API_SECRET_PLUGIN_FITBIT, "033ed2a3710c0cde04343d073c09e378");

            if (fitbitAPI != null || fitbitOAUTHToken != null) {
                try {
                    Scheduler.Schedule fitbitFetcher = Scheduler.getSchedule(this, SCHEDULER_PLUGIN_FITBIT);
                    if (fitbitFetcher == null) {
                        fitbitFetcher = new Scheduler.Schedule(SCHEDULER_PLUGIN_FITBIT);
                        fitbitFetcher.setInterval(Long.valueOf(Aware.getSetting(this, Settings.PLUGIN_FITBIT_FREQUENCY)))
                                .setActionType(Scheduler.ACTION_TYPE_SERVICE)
                                .setActionClass(getPackageName() + "/" + getClass().getName())
                                .setActionIntentAction(ACTION_AWARE_PLUGIN_FITBIT_SYNC);

                        Scheduler.saveSchedule(this, fitbitFetcher);
                    } else {
                        if (fitbitFetcher.getInterval() != Long.valueOf(Aware.getSetting(this, Settings.PLUGIN_FITBIT_FREQUENCY))) {
                            fitbitFetcher.setInterval(Long.valueOf(Aware.getSetting(this, Settings.PLUGIN_FITBIT_FREQUENCY)));
                            Scheduler.saveSchedule(this, fitbitFetcher);
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

//            if (Aware.getSetting(this, Settings.OAUTH_TOKEN).length() == 0) {
//                Intent settingsUI = new Intent(this, Settings.class);
//                settingsUI.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                startActivity(settingsUI);
//            }
        } else {
            Intent permissions = new Intent(this, PermissionsHandler.class);
            permissions.putExtra(PermissionsHandler.EXTRA_REQUIRED_PERMISSIONS, REQUIRED_PERMISSIONS);
            permissions.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(permissions);
        }

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        Aware.setSetting(this, Settings.STATUS_PLUGIN_FITBIT, false);

        //Stop AWARE
        Aware.stopAWARE(this);
    }
}
