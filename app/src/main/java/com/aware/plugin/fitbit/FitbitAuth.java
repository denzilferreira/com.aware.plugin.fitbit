package com.aware.plugin.fitbit;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.aware.Aware;
import com.github.scribejava.core.model.OAuth2AccessToken;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by denzil on 10/01/2017.
 */

public class FitbitAuth extends AppCompatActivity {

    @Override
    protected void onResume() {
        super.onResume();

        if (Aware.getSetting(this, Settings.OAUTH_TOKEN).length() == 0) {
            try {
                Plugin.AuthorizeFitbit(this);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {
            Toast.makeText(getApplicationContext(), "Authentication OK!", Toast.LENGTH_SHORT).show();
        }
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

                //the ones the user has accepted to share
                Aware.setSetting(this, Settings.OAUTH_SCOPES, data_scopes.toString());

            } catch (JSONException e) {
                e.printStackTrace();
            }

            Aware.setSetting(this, Settings.OAUTH_TOKEN_TYPE, token_Type);
            Aware.setSetting(this, Settings.OAUTH_VALIDITY, expires_In);

            Plugin.fitbitOAUTHToken = new OAuth2AccessToken(Aware.getSetting(this, Settings.OAUTH_TOKEN),
                    Aware.getSetting(this, Settings.OAUTH_TOKEN_TYPE),
                    Integer.valueOf(Aware.getSetting(this, Settings.OAUTH_VALIDITY)),
                    "null",
                    Aware.getSetting(this, Settings.OAUTH_SCOPES),
                    "null");

            //Ask the user to pick the Fitbit they will use
            new Plugin.FitbitDevicesPicker(getApplicationContext()).execute();
        }
    }
}
