package com.aware.plugin.Fitbit;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.customtabs.CustomTabsIntent;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Response;
import com.github.scribejava.core.model.Verb;
import com.github.scribejava.core.oauth.OAuth20Service;

import java.io.IOException;

/**
 * Created by sklakegg on 13/12/16.
 */

public class Fitbit extends AppCompatActivity {

    // Retrieved from Developer Fitbit
    final String api_Key = "227YG3";
    final String api_Secret = "033ed2a3710c0cde04343d073c09e378";
    final String auth_scope = "activity";
    final String response = "token";
    final String redirect_URI = "Fitbit://logincallback";

    // Defined by the redirect URI fragment
    String access_Token;
    String userID;
    String data_scope;
    String token_Type;
    int expires_In;

    // Misc
    final private String Fitbit_Preference = "Fitbit_Preference";
    String responseString;
    OAuth20Service OA2_Service;
    OAuth2AccessToken OA2_Access_Token;
    SharedPreferences prefs;
    Button button;
    //Spinner languageSpinner;
    Spinner unitSpinner;
    String languageSelected;
    String unitSelected;
    String unit_Array [] = {"en_US", "en_GB", "Metric"};
    //ArrayAdapter<CharSequence> language_Adapter;
    ArrayAdapter<CharSequence> unit_Adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.card);

        // Get access token and preferences.
        prefs = getSharedPreferences(Fitbit_Preference, MODE_PRIVATE);
        access_Token = prefs.getString("OA2_Access_Token", null);
        languageSelected = prefs.getString("languageSelected", null);
        unitSelected = prefs.getString("unitSelected", null);

        // Select preferences.

        // Language not yet supported by Fitbit.
        //languageSpinner = (Spinner) findViewById(R.id.spinner_Language);
        unitSpinner = (Spinner) findViewById(R.id.spinner_Unit);

        //language_Adapter = ArrayAdapter.createFromResource(this, R.array.spinner_Language, android.R.layout.simple_spinner_item);
        //language_Adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        //languageSpinner.setAdapter(language_Adapter);
        /* Set up spinner
        if (languageSelected != null) { languageSpinner.setSelection(language_Adapter.getPosition(languageSelected)); }
        else { languageSpinner.setSelection(language_Adapter.getPosition("United States")); }
        languageSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                languageSelected = adapterView.getItemAtPosition(i).toString();
                SharedPreferences.Editor editor = getSharedPreferences(Fitbit_Preference, MODE_PRIVATE).edit();
                editor.putString("languageSelected", languageSelected);
                editor.commit();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });
        */

        // Inflate spinner and get selections.
        unit_Adapter = ArrayAdapter.createFromResource(this, R.array.spinner_Units, android.R.layout.simple_spinner_item);
        unit_Adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        unitSpinner.setAdapter(unit_Adapter);
        // Set up spinner
        if (unitSelected != null) { unitSpinner.setSelection(unit_Adapter.getPosition(unitSelected)); }
        else { unitSpinner.setSelection(unit_Adapter.getPosition("Metric")); }
        unitSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                unitSelected = String.valueOf(i);
                SharedPreferences.Editor editor = getSharedPreferences(Fitbit_Preference, MODE_PRIVATE).edit();
                editor.putString("unitSelected", String.valueOf(i));
                editor.commit();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });

        button = (Button) findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getData();
            }
        });

        if (access_Token == null) {
            AuthenticateClient();
        }
        else {
            access_Token = prefs.getString("OA2_Access_Token", null);
            token_Type = prefs.getString("Token Type", null);
            data_scope = prefs.getString("Data Scope", null);
            OA2_Access_Token = new OAuth2AccessToken(access_Token, token_Type, expires_In, "null", data_scope, "null");
        }

        // Build service.
        OA2_Service = new ServiceBuilder()
                .apiKey(api_Key)
                .scope(auth_scope)
                .responseType(response)
                .callback(redirect_URI)
                .apiSecret(api_Secret)
                .build(FitbitAPI.instance());
    }

    private void AuthenticateClient() {
        // Launch Chrome Custom Tab.
        final String authorizationUrl = OA2_Service.getAuthorizationUrl();
        CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
        CustomTabsIntent customTabsIntent = builder.build();
        customTabsIntent.launchUrl(this, Uri.parse(authorizationUrl));
    }

    // Called on redirect URI.
    protected void onNewIntent(Intent intent) {
        if(intent.getData() != null) {
            Uri URL_Fragment = intent.getData();
            String URL_Fragment_String = URL_Fragment.toString();

            access_Token = URL_Fragment_String.substring(URL_Fragment_String.indexOf("access_token=") + 13, URL_Fragment_String.indexOf("&user_id"));
            userID = URL_Fragment_String.substring(URL_Fragment_String.indexOf("user_id=") + 8, URL_Fragment_String.indexOf("&scope"));
            data_scope = URL_Fragment_String.substring(URL_Fragment_String.indexOf("scope=") + 6, URL_Fragment_String.indexOf("&token_type"));
            token_Type = URL_Fragment_String.substring(URL_Fragment_String.indexOf("token_type=") + 11, URL_Fragment_String.indexOf("&expires_in"));
            expires_In = Integer.parseInt(URL_Fragment_String.substring(URL_Fragment_String.indexOf("expires_in=") + 11, URL_Fragment_String.length()));

            OA2_Access_Token = new OAuth2AccessToken(access_Token, token_Type, expires_In, "null", data_scope, "null");

            SharedPreferences.Editor editor = getSharedPreferences(Fitbit_Preference, MODE_PRIVATE).edit();
            editor.putString("OA2_Access_Token", access_Token);
            editor.putString("Data Scope", data_scope);
            editor.putString("Token Type", token_Type);
            editor.putString("Expires In", Integer.toString(expires_In));
            Log.d("Expires In", Integer.toString(expires_In));

            editor.commit();
        }
    }

    public void getData() {
        new Thread(new Runnable() {
            public void run() {

                final OAuthRequest request = new OAuthRequest(Verb.GET, "https://api.fitbit.com/1/user/-/activities/date/2016-12-13.json", OA2_Service);
                request.addHeader("Authorization", " " + token_Type + " " + access_Token);

                // Language not yet supported by Fitbit.
                //request.addHeader("Accept-Locale",  "en_US");

                // Select unit type.
                if (unit_Array[Integer.parseInt(unitSelected)] != null) {
                    request.addHeader("Accept-Language",  unit_Array[Integer.parseInt(unitSelected)]);
                }

                OA2_Service.signRequest(OA2_Access_Token, request);
                final Response response = request.send();

                try {
                    responseString = response.getBody().toString();
                    // Add check for remaining cases.
                    if(responseString.indexOf("expired_token") > 0 || responseString.indexOf("insufficient_scope") > 0) {
                        //AuthenticateClient();
                        //getData();
                        Log.d("Response", responseString);
                    }
                    else {
                        Log.d("Response", responseString);
                        /*
                        reader = new JSONObject(responseString);
                        JSONObject user  = reader.getJSONObject("user");

                        ContentValues rowData = new ContentValues();
                        //rowData.put(Provider.TableOne_Data._ID, "123");
                        rowData.put(Provider.TableOne_Data.TIMESTAMP, System.currentTimeMillis());
                        rowData.put(Provider.TableOne_Data.DEVICE_ID, "2dca4920-8a8f-48ad-b1fd-a8c5a4668128");
                        rowData.put(Provider.TableOne_Data.NAME, user.toString());
                        rowData.put(Provider.TableOne_Data.BIG_NUMBER, "123");
                        rowData.put(Provider.TableOne_Data.PICTURE, "123");

                        //getContentResolver().insert(Bluetooth_Data.CONTENT_URI);
                        getContentResolver().insert(Provider.CONTENT_URI, rowData);*/
                        //Provider fitbitProvider = new Provider();
                        //fitbitProvider.initializeDB();
                        //fitbitProvider.insert(Provider.CONTENT_URI, rowData);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } /*catch (JSONException e) {
                    e.printStackTrace();
                }*/
            }
        }).start();
    }
}


