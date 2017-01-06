package com.aware.plugin.Fitbit;

import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
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
import android.widget.TextView;

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
import java.util.Calendar;
import java.util.Date;

/**
 * Created by sklakegg on 13/12/16.
 */

public class Fitbit extends AppCompatActivity {

    // Retrieved from  Fitbit
    final String api_Key = "227YG3";
    final String api_Secret = "033ed2a3710c0cde04343d073c09e378";
    String auth_scope;
    final String response = "token";
    final String redirect_URI = "Fitbit://logincallback";
    String responseString;
    String string_Activity;
    String string_HR;
    String string_Sleep;
    String lastSynced;
    String lastSynced_MinusH;
    String stepsLastHours = "";

    // Defined by the redirect URI fragment
    String access_Token;
    String userID;
    String data_scope;
    String token_Type;
    int expires_In;

    // Shared preferences.
    SharedPreferences prefs;
    final private String Fitbit_Preference = "Fitbit_Preference";

    // Scribe variables.
    OAuth20Service OA2_Service;
    OAuth2AccessToken OA2_Access_Token;

    // UI.
    Button button1;
    Button button2;
    Button button3;
    Spinner unitSpinner;
    CheckBox checkbox_Activity;
    CheckBox checkbox_HR;
    CheckBox checkbox_Sleep;
    TextView stepsLast3Hours;
    TextView stepsLast5Hours;
    int unitSelected;
    String unit_Array [] = {"en_US", "en_GB", "Metric"};
    ArrayAdapter<CharSequence> unit_Adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.card);

        JodaTimeAndroid.init(this);
        unitSpinner = (Spinner) findViewById(R.id.spinner_Unit);
        checkbox_Activity = (CheckBox) findViewById(R.id.check_Activity);
        checkbox_HR = (CheckBox) findViewById(R.id.check_HR);
        checkbox_Sleep = (CheckBox) findViewById(R.id.check_Sleep);
        button1 = (Button) findViewById(R.id.button);
        button2 = (Button) findViewById(R.id.button2);
        button3 = (Button) findViewById(R.id.button3);
        stepsLast3Hours = (TextView) findViewById(R.id.textView);
        stepsLast5Hours = (TextView) findViewById(R.id.textView4);

        prefs = getSharedPreferences(Fitbit_Preference, MODE_PRIVATE);
        access_Token = prefs.getString("OA2_Access_Token", null);

        checkAuthorization();

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
                if(isChecked) {
                    editor.putBoolean("Activity_Checked", true);
                }
                if(!isChecked) {
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
                if(isChecked) {
                    editor.putBoolean("HR_Checked", true);
                }
                if(!isChecked) {
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
                if(isChecked) {
                    editor.putBoolean("Sleep_Checked", true);
                }
                if(!isChecked) {
                    editor.putBoolean("Sleep_Checked", false);
                }
                editor.commit();
                checkAuthorization();
            }
        });

        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!checkAuthorization()) { AuthorizeClient(); }
            }
        });

        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(checkAuthorization() && prefs.getString("OA2_Access_Token", null) != null) { getFitbitData(); }
            }
        });

        button3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(
                    prefs.getBoolean("Activity_Checked", false) == prefs.getBoolean("access_Token_Activity", false) &&
                    prefs.getBoolean("Activity_Checked", false) &&
                    prefs.getBoolean("access_Token_Activity", false) &&
                    prefs.getBoolean("access_Token_Settings", false) &&
                    prefs.getString("OA2_Access_Token", null) != null)
                {
                    button3.setBackgroundColor(Color.GREEN);
                    //hours = 3;
                    //String lol = stepCounterLastHours(5);
                    //Log.d("ABC 3" , lol);
                    //l/ol = stepCounterLastHours(3);
                    //Log.d("ABC 5" , "HAHAHA");
                    new LongOperation().execute("3");
                    new LongOperation().execute("5");

//                    Log.d("ABC 5" , lol);
                }
                else { button3.setBackgroundColor(Color.RED); }
            }
        });
    }

    // Check if the user's requested scope equals the scope of the access token.
    public boolean checkAuthorization() {

        if(prefs.getBoolean("Activity_Checked", false) == prefs.getBoolean("access_Token_Activity", false)
                && prefs.getBoolean("HR_Checked", false) == prefs.getBoolean("access_Token_HR", false)
                && prefs.getBoolean("Sleep_Checked", false) == prefs.getBoolean("access_Token_Sleep", false))
        { button1.setBackgroundColor(Color.GREEN); return true;}
        else { button1.setBackgroundColor(Color.RED); return false;}
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

        // Build service.
        OA2_Service = buildOAuth20Service();

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

            Log.d("ABC", URL_Fragment_String);

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
            else { editor.putBoolean("access_Token_Activity", false); }

            if(data_scope.toLowerCase().contains("heartrate".toLowerCase())) {
                editor.putBoolean("access_Token_HR", true);
            }
            else { editor.putBoolean("access_Token_HR", false); }

            if(data_scope.toLowerCase().contains("sleep".toLowerCase())) {
                editor.putBoolean("access_Token_Sleep", true);
            }
            else { editor.putBoolean("access_Token_Sleep", false); }

            if(data_scope.toLowerCase().contains("settings".toLowerCase())) {
                editor.putBoolean("access_Token_Settings", true);
            }
            else { editor.putBoolean("access_Token_Settings", false); }

            editor.putString("OA2_Access_Token", access_Token);
            editor.putString("Token_Data_Scope", data_scope);
            editor.putString("Token Type", token_Type);
            editor.putInt("Expires In", expires_In);
            editor.commit();

            button1.setBackgroundColor(Color.GREEN);
        }
    }

    // Retrieve data from fitbit.
    public void getFitbitData() {

        new Thread(new Runnable() {
            public void run() {

                // Create the OAuth2.0 access token.
                OA2_Access_Token = createOAuth2AccessToken();

                // Retrieve the time and date of when the user last synced data.
                lastSynced = requestSend("https://api.fitbit.com/1/user/-/devices.json");
                try {
                    JSONArray lastSynced_JSONArr = new JSONArray(lastSynced);
                    JSONObject lastSynced_JSONObj = lastSynced_JSONArr.getJSONObject(0);
                    lastSynced = lastSynced_JSONObj.getString("lastSyncTime");
                    String[] parts = lastSynced.split("T");
                    String lastSynced_Date = parts[0];
                    String lastSynced_Time = parts[1];
                    lastSynced_Time = lastSynced_Time.substring(0, lastSynced_Time.lastIndexOf(":"));

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                // Send request for data.
                if (prefs.getBoolean("access_Token_Activity", false)) {

                    string_Activity = requestSend("https://api.fitbit.com/1/user/-/activities/steps/date/today/1d/15min.json");
                    //Log.d("Activity", string_Activity);
                    //storeData(string_Activity, "Activity");
                }

                if (prefs.getBoolean("access_Token_HR", false)) {
                    string_HR = requestSend("https://api.fitbit.com/1/user/-/activities/heart/date/today/1d/1min.json");
                    //Log.d("HR", string_HR);
                    //storeData(string_HR, "HR");
                }

                if (prefs.getBoolean("access_Token_Sleep", false)) {
                    string_Sleep = requestSend("https://api.fitbit.com/1/user/-/sleep/date/2016-12-13.json");
                    //Log.d("Sleep", string_Sleep);
                    //storeData(string_Sleep, "Sleep");
                }
            }
        }).start();
    }

    private class LongOperation extends AsyncTask<String, Void, String> {

        int hours;

        @Override
        protected String doInBackground(String... params) {
            String myString = params[0];
            hours = Integer.parseInt(myString);
            OA2_Access_Token = createOAuth2AccessToken();
            lastSynced = requestSend("https://api.fitbit.com/1/user/-/devices.json");

            try {
                // Retrieve the time and date of when the user last synced data.
                JSONArray lastSynced_JSONArr = new JSONArray(lastSynced);
                JSONObject lastSynced_JSONObj = lastSynced_JSONArr.getJSONObject(0);
                lastSynced = lastSynced_JSONObj.getString("lastSyncTime");
                String[] parts = lastSynced.split("T");
                String lastSynced_Date = parts[0];
                String lastSynced_Time = parts[1];
                lastSynced_Time = lastSynced_Time.substring(0, lastSynced_Time.lastIndexOf(":"));
                DateTime lastSynced_DateTime = DateTime.parse(lastSynced);

                // Retrieve the time and date X hours before the user last synced data.
                DateTime lastSynced_DateTime_MinusH = lastSynced_DateTime.minusHours(hours);
                lastSynced_MinusH = lastSynced_DateTime_MinusH.toString();
                lastSynced_MinusH = lastSynced_MinusH.substring(0, lastSynced_MinusH.lastIndexOf("+"));
                parts = lastSynced_MinusH.split("T");
                String lastSynced_MinusH_Date = parts[0];
                String lastSynced_MinusH_Time = parts[1];
                lastSynced_MinusH_Time = lastSynced_MinusH_Time.substring(0, lastSynced_MinusH_Time.lastIndexOf(":"));

                // Request data in this time period.
                stepsLastHours = requestSend(
                        "https://api.fitbit.com/1/user/-/activities/steps/date/"
                                + lastSynced_MinusH_Date + "/" + lastSynced_Date + "/15min/time/"
                                + lastSynced_MinusH_Time + "/" + lastSynced_Time + ".json");

                // Parse JSON and return string.
                JSONObject obj = new JSONObject(stepsLastHours);
                JSONArray stepsArray = obj.getJSONArray("activities-steps");
                JSONObject stepsObject = stepsArray.getJSONObject(0);
                stepsLastHours = stepsObject.getString("value");

            } catch (JSONException e) {
                e.printStackTrace();
            }

            return stepsLastHours;
        }

        @Override
        protected void onPostExecute(String result) {
            //TextView txt = (TextView) findViewById(R.id.output);
            //txt.setText("Executed"); // txt.setText(result);
            // might want to change "executed" for the returned string passed
            // into onPostExecute() but that is upto you
            Log.d("ABC 1111", stepsLastHours);
            if(hours == 3) {stepsLast3Hours.setText(stepsLastHours);}
            if(hours == 5) {stepsLast5Hours.setText(stepsLastHours);}
        }
    }


    // Request step count in last x hours.
    // OBS - CAN NOT BE GREATER THEN 24 HOURS!
    String stepCounterLastHours(int hours) {
        class stepCounterLastHoursClass implements Runnable {
            int hours;
            stepCounterLastHoursClass(int hours) {
                this.hours = hours;
            }
            public void run() {
                OA2_Access_Token = createOAuth2AccessToken();
                lastSynced = requestSend("https://api.fitbit.com/1/user/-/devices.json");

                try {
                    // Retrieve the time and date of when the user last synced data.
                    JSONArray lastSynced_JSONArr = new JSONArray(lastSynced);
                    JSONObject lastSynced_JSONObj = lastSynced_JSONArr.getJSONObject(0);
                    lastSynced = lastSynced_JSONObj.getString("lastSyncTime");
                    String[] parts = lastSynced.split("T");
                    String lastSynced_Date = parts[0];
                    String lastSynced_Time = parts[1];
                    lastSynced_Time = lastSynced_Time.substring(0, lastSynced_Time.lastIndexOf(":"));
                    DateTime lastSynced_DateTime = DateTime.parse(lastSynced);

                    // Retrieve the time and date X hours before the user last synced data.
                    DateTime lastSynced_DateTime_MinusH = lastSynced_DateTime.minusHours(hours);
                    lastSynced_MinusH = lastSynced_DateTime_MinusH.toString();
                    lastSynced_MinusH = lastSynced_MinusH.substring(0, lastSynced_MinusH.lastIndexOf("+"));
                    parts = lastSynced_MinusH.split("T");
                    String lastSynced_MinusH_Date = parts[0];
                    String lastSynced_MinusH_Time = parts[1];
                    lastSynced_MinusH_Time = lastSynced_MinusH_Time.substring(0, lastSynced_MinusH_Time.lastIndexOf(":"));

                    // Request data in this time period.
                    stepsLastHours = requestSend(
                            "https://api.fitbit.com/1/user/-/activities/steps/date/"
                                    + lastSynced_MinusH_Date + "/" + lastSynced_Date + "/15min/time/"
                                    + lastSynced_MinusH_Time + "/" + lastSynced_Time + ".json");

                    // Parse JSON and return string.
                    JSONObject obj = new JSONObject(stepsLastHours);
                    JSONArray stepsArray = obj.getJSONArray("activities-steps");
                    JSONObject stepsObject = stepsArray.getJSONObject(0);
                    stepsLastHours = stepsObject.getString("value");

                } catch (JSONException e) {
                    e.printStackTrace();
                }            }
        }
        Thread t = new Thread(new stepCounterLastHoursClass(hours));
        t.start();
        return stepsLastHours;
    }

    // Create request, add headers and send.
    public String requestSend(String request_URL) {

        OA2_Service = buildOAuth20Service();

        final OAuthRequest request = new OAuthRequest(Verb.GET, request_URL, OA2_Service);
        request.addHeader("Authorization", " " + prefs.getString("Token Type", null) + " " + prefs.getString("OA2_Access_Token", null));
        request.addHeader("Accept-Language",  unit_Array[prefs.getInt("unitSelected", 2)]);

        OA2_Service.signRequest(OA2_Access_Token, request);
        final Response response = request.send();
        try {
            responseString = response.getBody().toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return responseString;
    }

    public OAuth20Service buildOAuth20Service() {

        // Get the data scope from shared preferences.
        auth_scope = "";
        if (prefs.getBoolean("Activity_Checked", false)) { auth_scope = "activity"; }
        if (prefs.getBoolean("HR_Checked", false)) { auth_scope =  auth_scope + " " + "heartrate"; }
        if (prefs.getBoolean("Sleep_Checked", false)) { auth_scope =  auth_scope + " " + "sleep"; }

        // Build service.
        OA2_Service = new ServiceBuilder()
                .apiKey(api_Key)
                .scope(auth_scope + " settings")
                .responseType(response)
                .callback(redirect_URI)
                .apiSecret(api_Secret)
                .build(FitbitAPI.instance());

        return OA2_Service;
    }

    // Create a new OAuth2 access token.
    public OAuth2AccessToken createOAuth2AccessToken() {
        OA2_Access_Token = new OAuth2AccessToken(
                prefs.getString("OA2_Access_Token", null),
                prefs.getString("Token Type", null),
                prefs.getInt("Expires In", 0),
                "null",
                prefs.getString("Token_Data_Scope", null),
                "null");
        return OA2_Access_Token;
    }

    // Store the Fitbit data.
    public void storeData(String responseData, String type) {

        ContentValues rowData = new ContentValues();
        rowData.put(Provider.TableOne_Data.TIMESTAMP, System.currentTimeMillis());
        rowData.put(Provider.TableOne_Data.DEVICE_ID, "2dca4920-8a8f-48ad-b1fd-a8c5a4668128");
        rowData.put(Provider.TableOne_Data.FITBIT_JSON, responseData);
        rowData.put(Provider.TableOne_Data.DATA_TYPE, type);
        getContentResolver().insert(Provider.CONTENT_URI, rowData);
    }
}