package com.aware.plugin.fitbit;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.customtabs.CustomTabsIntent;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
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

import org.json.JSONException;

import java.io.IOException;

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
            //TODO: fetch fitbit data
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

            if (Aware.getSetting(getApplicationContext(), Settings.OAUTH_TOKEN).length() > 0) {

                //TODO device picker
                //Ask the user to pick the Fitbit they will use
                new Plugin.FitbitDevicesPicker(getApplicationContext()).execute();

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
            } else {
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
     * Initiate authorization flow with Fitbit API
     *
     * @param context
     * @throws JSONException
     */
    public static void AuthorizeFitbit(Context context) throws JSONException {
        String scopes = "activity heartrate sleep settings";

        Plugin.fitbitAPI = new ServiceBuilder()
                .apiKey(Aware.getSetting(context, Settings.API_KEY_PLUGIN_FITBIT))
                .scope(scopes)
                .responseType("token")
                .callback("fitbit://logincallback")
                .apiSecret(Aware.getSetting(context, Settings.API_SECRET_PLUGIN_FITBIT))
                .build(FitbitAPI.instance());

        CustomTabsIntent customTabsIntent = new CustomTabsIntent.Builder().build();
        customTabsIntent.launchUrl(context, Uri.parse(Plugin.fitbitAPI.getAuthorizationUrl()));
    }

    /**
     * Ask the user to choose which Fitbit will be used with the plugin
     * ONLY CALL THIS IF {@link Settings#OAUTH_TOKEN}.length > 0
     */
    public static class FitbitDevicesPicker extends AsyncTask<Void, Void, Boolean> {
        private Context mContext;

        FitbitDevicesPicker(Context context) {
            mContext = context;
        }

        @Override
        protected Boolean doInBackground(Void... params) {

            if (Aware.getSetting(mContext, Settings.OAUTH_TOKEN).length() == 0) return false;

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
