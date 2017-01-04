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
import android.widget.CheckBox;
import android.widget.CompoundButton;
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
    String auth_scope;
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
    Button button2;
    Spinner unitSpinner;
    CheckBox checkbox_Activity;
    CheckBox checkbox_HR;
    CheckBox checkbox_Sleep;
    String languageSelected;
    int unitSelected;
    String unit_Array [] = {"en_US", "en_GB", "Metric"};
    ArrayAdapter<CharSequence> unit_Adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.card);

        // Get access token and preferences.
        prefs = getSharedPreferences(Fitbit_Preference, MODE_PRIVATE);
        access_Token = prefs.getString("OA2_Access_Token", null);
        languageSelected = prefs.getString("languageSelected", null);
        unitSelected = prefs.getInt("unitSelected", 2);

        // Select preferences.

        // Language not yet supported by Fitbit.
        //languageSpinner = (Spinner) findViewById(R.id.spinner_Language);
        unitSpinner = (Spinner) findViewById(R.id.spinner_Unit);
        checkbox_Activity = (CheckBox) findViewById(R.id.check_Activity);
        checkbox_HR = (CheckBox) findViewById(R.id.check_HR);
        checkbox_Sleep = (CheckBox) findViewById(R.id.check_Sleep);
        button = (Button) findViewById(R.id.button);

        checkAuthorization();

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
        unitSpinner.setSelection(unitSelected);

        unitSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                SharedPreferences.Editor editor = getSharedPreferences(Fitbit_Preference, MODE_PRIVATE).edit();
                editor.putInt("unitSelected", i);
                editor.commit();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });

        setButtonChecked();

        checkbox_Activity.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView,boolean isChecked) {
                SharedPreferences.Editor editor = getSharedPreferences(Fitbit_Preference, MODE_PRIVATE).edit();
                if(isChecked == true) {
                    editor.putBoolean("Activity_Checked", true);
                }
                if(isChecked == false) {
                    editor.putBoolean("Activity_Checked", false);
                }
                editor.commit();
                checkAuthorization();
            }
        });
        checkbox_HR.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView,boolean isChecked) {
                SharedPreferences.Editor editor = getSharedPreferences(Fitbit_Preference, MODE_PRIVATE).edit();
                if(isChecked == true) {
                    editor.putBoolean("HR_Checked", true);
                }
                if(isChecked == false) {
                    editor.putBoolean("HR_Checked", false);
                }
                editor.commit();
                checkAuthorization();
            }
        });
        checkbox_Sleep.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView,boolean isChecked) {
                SharedPreferences.Editor editor = getSharedPreferences(Fitbit_Preference, MODE_PRIVATE).edit();
                if(isChecked == true) {
                    editor.putBoolean("Sleep_Checked", true);
                }
                if(isChecked == false) {
                    editor.putBoolean("Sleep_Checked", false);
                }
                editor.commit();
                checkAuthorization();
            }
        });

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //getData();
            }
        });
    }

    // Check if the user's requested scope equals the scope of the access token.
    public void checkAuthorization() {

        if(prefs.getBoolean("Activity_Checked", false) == prefs.getBoolean("access_Token_Activity", false)
                && prefs.getBoolean("HR_Checked", false) == prefs.getBoolean("access_Token_HR", false)
                && prefs.getBoolean("Sleep_Checked", false) == prefs.getBoolean("access_Token_Sleep", false))
        { button.setBackgroundResource(R.drawable.ic_launcher); }
        else { button.setBackgroundResource(R.drawable.ic_action_accelerometer); }
    }

    // Put checkboxes in the correct state.
    public void setButtonChecked() {

        if (prefs.getBoolean("Activity_Checked", false)) { checkbox_Activity.setChecked(true); }
        else { checkbox_Activity.setChecked(false); }

        if (prefs.getBoolean("HR_Checked", false)) { checkbox_HR.setChecked(true); }
        else { checkbox_HR.setChecked(false); }

        if (prefs.getBoolean("Sleep_Checked", false)) { checkbox_Sleep.setChecked(true); }
        else { checkbox_Sleep.setChecked(false); }
    }

    // Called to authorize the client.
    public void AuthorizeClient () {

        // Get the data scope from shared preferences.
        auth_scope = "";
        if (prefs.getBoolean("Activity_Checked", false)) { auth_scope = "activity"; }
        if (prefs.getBoolean("HR_Checked", false)) { auth_scope =  auth_scope + " " + "heartrate"; }
        if (prefs.getBoolean("Sleep_Checked", false)) { auth_scope =  auth_scope + " " + "sleep"; }

        // Build service.
        OA2_Service = new ServiceBuilder()
                .apiKey(api_Key)
                .scope(auth_scope)
                .responseType(response)
                .callback(redirect_URI)
                .apiSecret(api_Secret)
                .build(FitbitAPI.instance());

        // Launch Chrome Custom Tab.
        final String authorizationUrl = OA2_Service.getAuthorizationUrl();
        CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
        CustomTabsIntent customTabsIntent = builder.build();
        customTabsIntent.launchUrl(this, Uri.parse(authorizationUrl));
    }

    // Called on redirect URI from Chrome Custom Tab. Creates new access token.
    protected void onNewIntent(Intent intent) {

        if(intent.getData() != null) {
            Uri URL_Fragment = intent.getData();
            String URL_Fragment_String = URL_Fragment.toString();

            // Retrieve information about access token.
            access_Token = URL_Fragment_String.substring(URL_Fragment_String.indexOf("access_token=") + 13, URL_Fragment_String.indexOf("&user_id"));
            userID = URL_Fragment_String.substring(URL_Fragment_String.indexOf("user_id=") + 8, URL_Fragment_String.indexOf("&scope"));
            data_scope = URL_Fragment_String.substring(URL_Fragment_String.indexOf("scope=") + 6, URL_Fragment_String.indexOf("&token_type"));
            token_Type = URL_Fragment_String.substring(URL_Fragment_String.indexOf("token_type=") + 11, URL_Fragment_String.indexOf("&expires_in"));
            expires_In = Integer.parseInt(URL_Fragment_String.substring(URL_Fragment_String.indexOf("expires_in=") + 11, URL_Fragment_String.length()));

            // Store information about access token in shared preferences.
            SharedPreferences.Editor editor = getSharedPreferences(Fitbit_Preference, MODE_PRIVATE).edit();

            if(data_scope.toLowerCase().contains("activity".toLowerCase())) {
                editor.putBoolean("access_Token_Activity", true);
            }
            if(data_scope.toLowerCase().contains("heartrate".toLowerCase())) {
                editor.putBoolean("access_Token_HR", true);
            }
            if(data_scope.toLowerCase().contains("sleep".toLowerCase())) {
                editor.putBoolean("access_Token_Sleep", true);
            }
            editor.putString("OA2_Access_Token", access_Token);
            editor.putString("Token_Data_Scope", data_scope);
            editor.putString("Token Type", token_Type);
            editor.putString("Expires In", Integer.toString(expires_In));
            editor.commit();
        }
    }

    // Retrieve data from fitbit.
    public void getFitbitData() {

        new Thread(new Runnable() {
            public void run() {

                // Create the OAuth2.0 access token.
                OA2_Access_Token = new OAuth2AccessToken(
                        prefs.getString("OA2_Access_Token", null),
                        prefs.getString("Token Type", null),
                        prefs.getInt("Expires In", 0),
                        "null",
                        prefs.getString("Token_Data_Scope", null),
                        "null");

                // Create request, add headers and send.
                final OAuthRequest request = new OAuthRequest(Verb.GET, "https://api.fitbit.com/1/user/-/activities/date/2016-12-13.json", OA2_Service);
                request.addHeader("Authorization", " " + token_Type + " " + access_Token);
                request.addHeader("Accept-Language",  unit_Array[prefs.getInt("unitSelected", 2)]);
                OA2_Service.signRequest(OA2_Access_Token, request);
                final Response response = request.send();

                /*
                try {
                    responseString = response.getBody().toString();
                    Log.d("Response", responseString);

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
                    getContentResolver().insert(Provider.CONTENT_URI, rowData);
                    //Provider fitbitProvider = new Provider();
                    //fitbitProvider.initializeDB();
                    //fitbitProvider.insert(Provider.CONTENT_URI, rowData);

                } catch (IOException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }*/
            }
        }).start();
    }
}